package de.catma.repository.git.graph.tp;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.Property;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.TagInstance;
import static de.catma.repository.git.graph.NodeType.Tagset;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasParent;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isAdjacentTo;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.Indexer;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;

public class TPGraphProjectHandler implements GraphProjectHandler {

	private Logger logger = Logger.getLogger(TPGraphProjectHandler.class.getName());
	private Graph graph;
	private ProjectReference projectReference;
	private User user;
	private FileInfoProvider fileInfoProvider;
	private TagManager tagManager;
	
	public TPGraphProjectHandler(ProjectReference projectReference, 
			User user, FileInfoProvider fileInfoProvider) {
		this.projectReference = projectReference;
		this.user = user;
		this.fileInfoProvider = fileInfoProvider;
		
		graph = TinkerGraph.open();
	}
	
	@Override
	public void ensureProjectRevisionIsLoaded(String revisionHash, TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier) throws Exception {
		
		GraphTraversalSource g = graph.traversal();

		if (!g.V().has(nt(ProjectRevision), "revisionHash", revisionHash).hasNext()) {
			((TinkerGraph)graph).clear();
			
			logger.info("Start loading " + projectReference.getName() + " " + projectReference.getProjectId());
			Vertex userV = graph.addVertex(nt(User));
			userV.property("userId", user.getIdentifier());
			
			Vertex projectV = graph.addVertex(nt(Project));
			projectV.property("propertyId", projectReference.getProjectId());
			
			userV.addEdge(rt(hasProject), projectV);
			
			Vertex projectRevV = graph.addVertex(nt(ProjectRevision));
			projectRevV.property("revisionHash", revisionHash);
			
			projectV.addEdge(rt(hasRevision), projectRevV);
			
			List<TagsetDefinition> tagsets = tagsetsSupplier.get();
			tagManager.load(tagsets);
			
			this.tagManager = tagManager;
			
			for (TagsetDefinition tagset : tagsets) {
				Vertex tagsetV = addTagset(projectRevV, tagset);
				addHasParentRelations(tagsetV, tagset);
			}
			
			for (SourceDocument document : documentsSupplier.get()) {
				addDocument(projectRevV, document);
			}

			for (UserMarkupCollection collection : collectionsSupplier.get(tagManager.getTagLibrary())) {
				addCollection(revisionHash, revisionHash, collection);
			}
			logger.info("Finished loading " + projectReference.getName() + " " + projectReference.getProjectId());
			
		}
	}

	private void addHasParentRelations(Vertex tagsetV, TagsetDefinition tagset) {
		if (!tagset.isEmpty()) {
			GraphTraversalSource g = graph.traversal();
			
			for (TagDefinition tag : tagset) {
				if (!tag.getParentUuid().isEmpty()) {
					Vertex tagV = g.V(tagsetV).outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid()).next();
				
					Vertex parentTagV = g.V(tagsetV).outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getParentUuid()).next();
					
					tagV.addEdge(rt(hasParent), parentTagV);
				}
			}
			
		}
	}

	private void addCollection(String oldRevisionHash, String revisionHash, UserMarkupCollection collection) {
		GraphTraversalSource g = graph.traversal();
		
		Vertex documentV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRevisionHash)
			.property("revisionHash", revisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", collection.getSourceDocumentId()).next();
		documentV
		.property("document")
		.ifPresent(
			doc -> 
				((SourceDocument)doc).addUserMarkupCollectionReference(
					new UserMarkupCollectionReference(
							collection.getUuid(),  
							collection.getRevisionHash(),  
							collection.getContentInfoSet(),  
							collection.getSourceDocumentId(), 
							collection.getSourceDocumentRevisionHash())));
		Vertex collectionV = graph.addVertex(nt(MarkupCollection));
		
		collectionV.property("collectionId", collection.getId());
//		collectionV.property("name", collection.getName());
//		collectionV.property("revisionHash", collection.getRevisionHash());
		collectionV.property("collection", collection);
		
		documentV.addEdge(rt(hasCollection), collectionV);
		
		addTagReferences(revisionHash, collectionV, collection.getTagReferences());
	}

	private void addTagReferences(String revisionHash, Vertex collectionV, List<TagReference> tagReferences) {
		final ArrayListMultimap<TagInstance, Range> tagInstancesAndRanges = ArrayListMultimap.create();
		
		tagReferences.forEach(tagReference -> {
			tagInstancesAndRanges.put(tagReference.getTagInstance(), tagReference.getRange());
		});
		
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
			
			GraphTraversalSource g = graph.traversal();

			Vertex tagV = 
				g.V().has(nt(ProjectRevision), "revisionHash", revisionHash)
				.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagsetId)
				.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tagId).next();

			tagV.addEdge(rt(hasInstance), tagInstanceV);
			
			for (Property property : ti.getUserDefinedProperties()) {
				Vertex annoPropertyV = graph.addVertex(nt(AnnotationProperty));
				annoPropertyV.property("uuid", property.getPropertyDefinitionId());
				annoPropertyV.property("values", property.getPropertyValueList());
				
				tagInstanceV.addEdge(rt(hasProperty), annoPropertyV);
			}
		}		
	}

	private void addDocument(Vertex projectRevV, SourceDocument document) throws Exception {
		logger.info("Starting to add Document " + document + " to the graph");
		Vertex documentV = graph.addVertex(nt(SourceDocument));
		SourceDocumentInfo info = 
			document.getSourceContentHandler().getSourceDocumentInfo();
		info.getTechInfoSet().setURI(fileInfoProvider.getSourceDocumentFileURI(document.getID()));
		documentV.property("documentId", document.getID());
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
			Path tokensPath = fileInfoProvider.getTokenizedSourceDocumentPath(document.getID());
			Any content = JsonIterator.deserialize(FileUtils.readFileToString(tokensPath.toFile(), "UTF-8"));
			Map<Integer, Vertex> adjacencyMap = new HashMap<>();
			for (Map.Entry<String, Any> entry : content.asMap().entrySet()) {
				String term = entry.getKey();
				Vertex termV = graph.addVertex(nt(Term));
				termV.property("literal", term);
				List<Any> positionList = entry.getValue().asList();
				termV.property("freq", positionList.size());
				
				termV.addEdge(rt(isPartOf), documentV);
				
				for (Any posEntry : positionList) {
					int startOffset = posEntry.get("startOffset").as(Integer.class);
					int endOffset = posEntry.get("endOffset").as(Integer.class);
					int tokenOffset = posEntry.get("tokenOffset").as(Integer.class);
					
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
					document.getID(), 
					projectReference.getProjectId()), 
				e);
		}
			
	}

	private Vertex addTagset(Vertex projectRevV, TagsetDefinition tagset) {
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

	private void addTag(Vertex tagsetV, Vertex parentTagV, TagDefinition tag) {
		Vertex tagV = graph.addVertex(nt(Tag));
		
		tagV.property("tagId", tag.getUuid());
//		tagV.property("author", tag.getAuthor());
//		tagV.property("color", tag.getColor());
		tagV.property("name", tag.getName());
		tagV.property("tag", tag);
		
		tagsetV.addEdge(rt(hasTag), tagV);
		
		if (parentTagV != null) {
			tagV.addEdge(rt(hasParent), parentTagV);
		}
		
		for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions()) {
			addPropertyDefinition(tagV, propertyDef);
		}
		
	}

	private void addPropertyDefinition(Vertex tagV, PropertyDefinition propertyDef) {
		Vertex propertyDefV = graph.addVertex(nt(Property));
		propertyDefV.property("uuid", propertyDef.getUuid());
		propertyDefV.property("name", propertyDef.getName());
		propertyDefV.property("values", propertyDef.getPossibleValueList());
		
		tagV.addEdge(rt(hasProperty), propertyDefV);
	}

	@Override
	public void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document,
			Path tokenizedSourceDocumentPath) throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex projectRevV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash).next();
		projectRevV.property("revisionHash", rootRevisionHash);
		
		addDocument(projectRevV, document);
	}

	@Override
	public Collection<SourceDocument> getDocuments(String rootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();

		return g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.properties("document")
		.map(prop -> (SourceDocument)prop.get().orElse(null))
		.toList();
	}

	@Override
	public SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception {
		GraphTraversalSource g = graph.traversal();

		return g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", sourceDocumentId)
		.properties("document")
		.map(prop -> (SourceDocument)prop.get().orElse(null))
		.next();
	}

	@Override
	public void addCollection(String rootRevisionHash, String collectionId, String name, String umcRevisionHash,
			SourceDocument document, String oldRootRevisionHash) throws Exception {
		addCollection(
			oldRootRevisionHash,
			rootRevisionHash,
			new UserMarkupCollection(collectionId, new ContentInfoSet(name), tagManager.getTagLibrary(), 
					document.getID(), document.getRevisionHash()));
	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex projectRevV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash).next();
		projectRevV.property("revisionHash", rootRevisionHash);
		
		Vertex tagsetV = addTagset(projectRevV, tagset);
		addHasParentRelations(tagsetV, tagset);
	}

	@Override
	public void addTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex tagsetV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
			.property("revisionHash", rootRevisionHash)
			.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid()).next();
		
		Vertex parentTagV = null;
		if (!tag.getParentUuid().isEmpty()) {
			parentTagV = g.V(tagsetV).outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getParentUuid()).next();
		}
		
		addTag(tagsetV, parentTagV, tag);
	}

	@Override
	public void updateTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		
		GraphTraversalSource g = graph.traversal();
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.property("name", tag.getName());

	}

	@Override
	public Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();

		return g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().hasLabel(nt(Tagset))
		.properties("tagset")
		.map(prop -> (TagsetDefinition)prop.get().orElse(null))
		.toList();
	}

	@Override
	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex tagV = g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.next();
		
		addPropertyDefinition(tagV, propertyDefinition);
	}

	@Override
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {

		GraphTraversalSource g = graph.traversal();

		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.outE(rt(hasProperty)).inV().has(nt(Property), "uuid", propertyDefinition.getUuid())
		.property("name", propertyDefinition.getName())
		.property("values", propertyDefinition.getPossibleValueList());
	}

	@Override
	public UserMarkupCollection getCollection(String rootRevisionHash, TagLibrary tagLibrary,
			UserMarkupCollectionReference collectionReference) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		return 
			g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", 
				collectionReference.getSourceDocumentId())
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collectionReference.getId())
			.properties("collection")
			.map(prop -> (UserMarkupCollection)prop.get().orElse(null))
			.next();
	}

	@Override
	public void addTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		System.out.println(graph);
		GraphTraversalSource g = graph.traversal();
		Vertex collectionV = 
			g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", 
				collection.getSourceDocumentId())
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collection.getId())
			.next();

		addTagReferences(rootRevisionHash, collectionV, tagReferences);
		System.out.println(graph);
	}

	@Override
	public void removeTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		
		System.out.println(graph);
		
		Set<String> tagInstanceIds = 
			tagReferences
				.stream()
				.map(tr -> tr.getTagInstanceID())
				.collect(Collectors.toSet());
		
		GraphTraversalSource g = graph.traversal();
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", 
			collection.getSourceDocumentId())
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collection.getId())
		.outE(rt(hasInstance)).inV().has(nt(TagInstance), "tagInstanceId", P.within(tagInstanceIds))
		.store("instances")
		.outE(rt(hasProperty)).inV().drop()
		.cap("instances").unfold().drop().iterate();
		
		System.out.println(graph);
	}

	@Override
	public void removeProperties(String rootRevisionHash, String collectionId, String collectionRevisionHash,
			String propertyDefId) throws Exception {
		GraphTraversalSource g = graph.traversal();
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collectionId)
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
		.outE(rt(hasProperty)).inV().has(nt(AnnotationProperty), "uuid", propertyDefId)
		.drop().iterate();
	}

	@Override
	public void updateProperties(
			String rootRevisionHash, 
			UserMarkupCollection collection, 
			TagInstance tagInstance, Collection<Property> properties) throws Exception {
	
		GraphTraversalSource g = graph.traversal();
		
		GraphTraversal<Vertex, Vertex> tagInstanceTraversal = g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", collection.getSourceDocumentId())
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collection.getId())
		.outE(rt(hasInstance)).inV().has(nt(TagInstance), "tagInstanceId", tagInstance.getUuid());
		
		for (Property property : properties) {
			tagInstanceTraversal
			.outE(rt(hasProperty)).inV().hasLabel(nt(Property))
			.property(property.getPropertyDefinitionId(), property.getPropertyValueList());
		}
	}
	

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash, TagDefinition tagDefinition)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(String rootRevisionHash,
			PropertyDefinition propertyDefinition, TagDefinition tag, TagLibrary tagLibrary) throws Exception {
		Multimap<String, TagReference> result = ArrayListMultimap.create();
		
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().hasLabel(nt(MarkupCollection))
		.properties("collection")
		.map(prop -> (UserMarkupCollection)prop.get().orElse(null))
		.toList()
		.forEach(collection -> result.putAll(collection.getId(), collection.getTagReferences(tag)));
		
		return result;
	}

	@Override
	public void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds,
			String collectionRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collectionId)
		.outE(rt(hasInstance)).inV().has(nt(TagInstance), "tagInstanceId", P.within(tagInstanceIds))
		.store("toBeDropped")
		.outE(rt(hasProperty)).inV().hasLabel(nt(AnnotationProperty)).drop() //AnnotationProperties
		.cap("toBeDropped").unfold().drop().iterate(); //TagInstance
		
	}

	@Override
	public void removeTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.store("toBeDropped")
		.outE(rt(hasProperty)).inV().hasLabel(nt(Property)).drop() // Properties
		.cap("toBeDropped").unfold().drop().iterate(); // Tag
	}

	@Override
	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.outE(rt(hasProperty)).inV().has(nt(Property), "uuid", propertyDefinition.getUuid())
		.drop().iterate();
	}

	@Override
	public void removeTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		GraphTraversalSource g = graph.traversal();

		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.store("toBeDropped")
		.outE(rt(hasTag)).inV().hasLabel(nt(Tag))
		.store("toBeDropped")
		.outE(rt(hasProperty)).inV().drop() // Properties
		.cap("toBeDropped").unfold().drop().iterate(); // Tagset and Tags	
	}

	@Override
	public void updateTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		GraphTraversalSource g = graph.traversal();
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tagset.getUuid())
		.property("name", tagset.getName());
	}

	@Override
	public void updateCollection(String rootRevisionHash, UserMarkupCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash);
		
		// the collection itself has currently no indexed fields so there is no updated to be done
	}

	@Override
	public void removeCollection(String rootRevisionHash, UserMarkupCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", collectionReference.getSourceDocumentId())
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collectionReference.getId())
		.store("toBeDropped")
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
		.store("toBeDropped")
		.outE(rt(hasProperty)).inV().hasLabel(nt(AnnotationProperty)).drop() //AnnotationProperties
		.cap("toBeDropped").unfold().drop().iterate(); // Collection and TagInstances		

	}

	@Override
	public void removeDocument(String rootRevisionHash, SourceDocument document, String oldRootRevisionHash)
			throws Exception {

		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", document.getID())
		.store("toBeDropped")
		.inE(rt(isPartOf)).outV().hasLabel(nt(Term))
		.store("toBeDropped")
		.outE(rt(hasPosition)).inV().hasLabel(nt(Position)).drop() // Positions
		.cap("toBeDropped").unfold().drop().iterate();  // Document and Terms
	}
	
	public Indexer createIndexer() {
		return new TPGraphProjectIndexer(graph);
	}

}
