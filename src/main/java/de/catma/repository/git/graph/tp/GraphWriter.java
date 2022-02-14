package de.catma.repository.git.graph.tp;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.Property;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.TagInstance;
import static de.catma.repository.git.graph.NodeType.Tagset;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasParent;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isAdjacentTo;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;

import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;

class GraphWriter {
	
	private final Logger logger = Logger.getLogger(GraphWriter.class.getName());
	private final Graph graph;
	private final FileInfoProvider fileInfoProvider;
	private final ProjectReference projectReference;
	private final User user;
	
	
	public GraphWriter(Graph graph, FileInfoProvider fileInfoProvider, ProjectReference projectReference, User user) {
		super();
		this.graph = graph;
		this.fileInfoProvider = fileInfoProvider;
		this.projectReference = projectReference;
		this.user = user;
	}

	Vertex addTagset(Vertex projectRevV, TagsetDefinition tagset) {
		Vertex tagsetV = graph.addVertex(nt(Tagset));
		
		tagsetV.property("tagsetId", tagset.getUuid());
		tagsetV.property("name", tagset.getName());
//		tagsetV.property("revisionHash", tagset.getRevisionHash());
//		tagsetV.property("description", "");// TODO: 
		tagsetV.property("tagset", tagset);
		
		projectRevV.addEdge(rt(hasTagset), tagsetV);
		
		for (TagDefinition tag : tagset) {			
			addTag(tagsetV, null, tag);
		}
		
		return tagsetV;
	}


	void addTag(Vertex tagsetV, Vertex parentTagV, TagDefinition tag) {
		Vertex tagV = graph.addVertex(nt(Tag));
		
		tagV.property("tagId", tag.getUuid());
//		tagV.property("author", tag.getAuthor());
//		tagV.property("color", tag.getColor());
		tagV.property("name", tag.getName());
		tagV.property("tag", tag);
		
		logVertex(tagV);
		
		tagsetV.addEdge(rt(hasTag), tagV);
		
		if (parentTagV != null) {
			tagV.addEdge(rt(hasParent), parentTagV);
		}
		
		for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions()) {
			addPropertyDefinition(tagV, propertyDef);
		}
		
	}
	
	void addPropertyDefinition(Vertex tagV, PropertyDefinition propertyDef) {
		Vertex propertyDefV = graph.addVertex(nt(Property));
		propertyDefV.property("uuid", propertyDef.getUuid());
		propertyDefV.property("name", propertyDef.getName());
		propertyDefV.property("values", propertyDef.getPossibleValueList());
		
//		logVertex(propertyDefV);
		
		tagV.addEdge(rt(hasProperty), propertyDefV);
	}
	
	
	void logVertex(Vertex v) {
		StringBuilder log = new StringBuilder(v.toString());
		log.append("label[");
		log.append(v.label());
		log.append("]");
		v.properties().forEachRemaining(prop -> log.append(" " + prop.key() + ":" + prop.value()));
		logger.info(log.toString());
	}

	void addHasParentRelations(Vertex tagsetV, TagsetDefinition tagset) {
		if (!tagset.isEmpty()) {
			GraphTraversalSource g = graph.traversal();
			
			for (TagDefinition tag : tagset) {
				if (!tag.getParentUuid().isEmpty()) {
					Vertex tagV = g.V(tagsetV).outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid()).next();
					GraphTraversal<Vertex, Vertex> traversal = g.V(tagsetV).outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getParentUuid());
					if (traversal.hasNext()) {
						Vertex parentTagV = traversal.next();
						
						tagV.addEdge(rt(hasParent), parentTagV);
					}
					else {
						throw new IllegalStateException(
							String.format(
								"Couldn't find parent for Tag %1$s with parent ID %2$s in Tagset %3$s", 
								tag.toString(), 
								tag.getParentUuid(),
								tagset.toString()));
					}
				}
			}
			
		}
	}
	
	void addDocument(Vertex projectRevV, SourceDocument document) throws Exception {
		logger.info("Starting to add Document " + document + " to the graph");
		Vertex documentV = graph.addVertex(nt(SourceDocument));
		SourceDocumentInfo info = 
			document.getSourceContentHandler().getSourceDocumentInfo();
		info.getTechInfoSet().setURI(fileInfoProvider.getSourceDocumentFileURI(document.getUuid()));
		documentV.property("documentId", document.getUuid());
//		documentV.property("author", info.getContentInfoSet().getAuthor());
//		documentV.property("description", info.getContentInfoSet().getDescription());
//		documentV.property("publisher", info.getContentInfoSet().getPublisher());
//		documentV.property("title", info.getContentInfoSet().getTitle());
//		documentV.property("checsum", info.getTechInfoSet().getChecksum());
//		documentV.property("charset", info.getTechInfoSet().getCharset());
//		documentV.property("fileOSType", info.getTechInfoSet().getFileOSType());
//		documentV.property("fileType", info.getTechInfoSet().getFileType());
//		documentV.property("mimeType", info.getTechInfoSet().getMimeType());
//		documentV.property("locale", info.getIndexInfoSet().getLocale());
		documentV.property("document", document);
		
		//TODO: necessary?
//		documentV.property("unseparableCharacterSequences", info.getIndexInfoSet().getUnseparableCharacterSequences());
//		documentV.property("userDefinedSeparatingCharacters", info.getIndexInfoSet().getUserDefinedSeparatingCharacters());
		
		projectRevV.addEdge(rt(hasDocument), documentV);
		
		try {
			Path tokensPath = fileInfoProvider.getTokenizedSourceDocumentPath(document.getUuid());
			@SuppressWarnings("rawtypes")
			Map content = new Gson().fromJson(FileUtils.readFileToString(tokensPath.toFile(), "UTF-8"), Map.class);
			
			Map<Integer, Vertex> adjacencyMap = new HashMap<>();
			for (Object entry : content.entrySet()) {
				
				String term = (String)((Map.Entry)entry).getKey();
				Vertex termV = graph.addVertex(nt(Term));
				termV.property("literal", term);
				List positionList = (List)((Map.Entry)entry).getValue();
				termV.property("freq", positionList.size());
				
				termV.addEdge(rt(isPartOf), documentV);
				
				for (Object posEntry : positionList) {
					int startOffset = ((Double)((Map)posEntry).get("startOffset")).intValue();
					int endOffset = ((Double)((Map)posEntry).get("endOffset")).intValue();
					int tokenOffset = ((Double)((Map)posEntry).get("tokenOffset")).intValue();
					
					Vertex positionV = graph.addVertex(nt(Position));
					positionV.property(
						"startOffset", startOffset);
					positionV.property( 
						"endOffset", endOffset);
					positionV.property(
						"tokenOffset", tokenOffset);
					
					termV.addEdge(rt(hasPosition), positionV);
					adjacencyMap.put(tokenOffset, positionV);
					
				}
			}
			for (int i=0; i<adjacencyMap.size()-1; i++) {
				adjacencyMap.get(i).addEdge(rt(isAdjacentTo), adjacencyMap.get(i+1));
			}
			logger.info("Finished adding Document " + document + " to the graph");	
		}
		catch (Exception e) {
			logger.log(
				Level.SEVERE, 
				String.format(
					"error loading tokens for Document %1$s in project %2$s", 
					document.getUuid(), 
					projectReference.getProjectId()), 
				e);
		}
			
	}
	
	void addCollection(String oldRevisionHash, String revisionHash, AnnotationCollection collection) {
		GraphTraversalSource g = graph.traversal();
		
		GraphTraversal<Vertex, Vertex> graphTraversal = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRevisionHash)
			.property("revisionHash", revisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", collection.getSourceDocumentId());
		
		if (graphTraversal.hasNext()) {
			Vertex documentV = 
				graphTraversal.next();
		
			documentV
			.property("document")
			.ifPresent(
				doc -> 
					((SourceDocument)doc).addUserMarkupCollectionReference(
						new AnnotationCollectionReference(
								collection.getUuid(),  
								collection.getContentInfoSet(),  
								collection.getSourceDocumentId(), 
								collection.getForkedFromCommitURL(),
								collection.getResponsableUser())));
			Vertex collectionV = graph.addVertex(nt(MarkupCollection));
			
			collectionV.property("collectionId", collection.getId());
	//		collectionV.property("name", collection.getName());
	//		collectionV.property("revisionHash", collection.getRevisionHash());
			collectionV.property("collection", collection);
			
			documentV.addEdge(rt(hasCollection), collectionV);
			
			addTagReferences(revisionHash, collectionV, collection.getTagReferences());
		}
		else {
			logger.info(
				String.format(
					"Skipping loading Collection %1$s with ID %2$s, couldn't find Document with ID %3$s", 
					collection.getName(), collection.getId(), collection.getSourceDocumentId()));
		}
	}
	


	void addTagReferences(String revisionHash, Vertex collectionV, List<TagReference> tagReferences) {
		final ArrayListMultimap<TagInstance, Range> tagInstancesAndRanges = ArrayListMultimap.create();
		
		tagReferences.forEach(tagReference -> {
			tagInstancesAndRanges.put(tagReference.getTagInstance(), tagReference.getRange());
		});

		Map<String, Vertex> tagNodesById = new HashMap<>();
		Set<String> availablePropertyDefIds = new HashSet<>();
		
		for (TagInstance ti : tagInstancesAndRanges.keySet()) {
			List<Range> ranges = tagInstancesAndRanges.get(ti);
			List<Integer> flatRanges = 
				ranges
				.stream()
				.sorted()
				.flatMap(range -> Stream.of(range.getStartPoint(), range.getEndPoint()))
				.collect(Collectors.toList());
			
			
			if (ti.getAuthor() == null) {
				ti.setAuthor(user.getIdentifier());
			}
			
			String tagsetId = ti.getTagsetId();
			String tagId = ti.getTagDefinitionId();
			
			Vertex tagInstanceV = graph.addVertex(nt(TagInstance));
			
			tagInstanceV.property("tagInstanceId", ti.getUuid());
			tagInstanceV.property("author", ti.getAuthor());
			tagInstanceV.property("timestamp", ti.getTimestamp());
			tagInstanceV.property("ranges", flatRanges);
			
			collectionV.addEdge(rt(hasInstance), tagInstanceV);
			
			Vertex tagV = tagNodesById.get(tagId);
			GraphTraversalSource g = graph.traversal();
			if (tagV == null) {
				GraphTraversal<Vertex, Vertex> traversal = 
						g.V().has(nt(ProjectRevision), "revisionHash", revisionHash)
						.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagsetId)
						.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tagId);
				if (traversal.hasNext()) {
					tagV = traversal.next();
					tagNodesById.put(tagId, tagV);
				}
			}
			

			if (tagV != null) {	// usually the Tag should always be present, 
										// because we delete stale Annotations when loading the Collection from git
										// if we hit an orphan Annotation at this stage it gets ignored
				                        // until the next sync might bring the corresponding Tag	
				tagV.addEdge(rt(hasInstance), tagInstanceV);
				
				for (Property property : ti.getUserDefinedProperties()) {
					
					if (availablePropertyDefIds.contains(property.getPropertyDefinitionId()) 
						|| g.V(tagV)
							.outE(rt(hasProperty))
							.inV().has(nt(Property), "uuid", property.getPropertyDefinitionId())
							.hasNext()) {
						Vertex annoPropertyV = graph.addVertex(nt(AnnotationProperty));
						annoPropertyV.property("uuid", property.getPropertyDefinitionId());
						annoPropertyV.property("values", property.getPropertyValueList());
						
						tagInstanceV.addEdge(rt(hasProperty), annoPropertyV);
						
						availablePropertyDefIds.add(property.getPropertyDefinitionId());
					}
				}
			}
		}		
	}


}
