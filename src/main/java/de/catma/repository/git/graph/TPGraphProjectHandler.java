package de.catma.repository.git.graph;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.Tagset;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isAdjacentTo;
import static de.catma.repository.git.graph.RelationType.rt;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.ProjectReference;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.IDGenerator;

public class TPGraphProjectHandler implements GraphProjectHandler {

	private Logger logger = Logger.getLogger(TPGraphProjectHandler.class.getName());
	private TinkerGraph graph;
	private ProjectReference projectReference;
	private User user;
	private FileInfoProvider fileInfoProvider;
	private IDGenerator idGenerator;
	
	public TPGraphProjectHandler(ProjectReference projectReference, 
			User user, FileInfoProvider fileInfoProvider) {
		this.projectReference = projectReference;
		this.user = user;
		this.fileInfoProvider = fileInfoProvider;
		this.idGenerator = new IDGenerator();
		
		graph = TinkerGraph.open();
	}
	
	@Override
	public void ensureProjectRevisionIsLoaded(String revisionHash, TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier) throws Exception {
		
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
		
		for (TagsetDefinition tagset : tagsets) {
			addTagset(projectRevV, tagset);
		}
		
		for (SourceDocument document : documentsSupplier.get()) {
			addDocument(projectRevV, document);
		}

		for (UserMarkupCollection collection : collectionsSupplier.get(tagManager.getTagLibrary())) {
			addCollection(revisionHash, collection);
		}
	}

	private void addCollection(String revisionHash, UserMarkupCollection collection) {
		GraphTraversalSource g = graph.traversal();
		
		Vertex documentV = 
			g.V().has(nt(ProjectRevision), "revisionHash", revisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", collection.getSourceDocumentId()).next();
		
		Vertex collectionV = graph.addVertex(nt(MarkupCollection));
		
		collectionV.property("collectionId", collection.getId());
		collectionV.property("name", collection.getName());
		collectionV.property("revisionHash", collection.getRevisionHash());
		collectionV.property("collection", collection);
		
		documentV.addEdge(rt(hasCollection), collectionV);
		
		final ArrayListMultimap<TagInstance, Range> tagInstancesAndRanges = ArrayListMultimap.create();
		
		collection.getTagReferences().forEach(tagReference -> {
			tagInstancesAndRanges.put(tagReference.getTagInstance(), tagReference.getRange());
		});
		
		for (TagInstance ti : tagInstancesAndRanges.keys()) {
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
			
			Vertex tagInstanceV = graph.addVertex(nt(NodeType.TagInstance));
			
			tagInstanceV.property("tagInstanceId", ti.getUuid());
			tagInstanceV.property("author", ti.getAuthor());
			tagInstanceV.property("creationDate", ti.getTimestamp());
			tagInstanceV.property("ranges", flatRanges);
			
			collectionV.addEdge(rt(hasInstance), tagInstanceV);
			
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

	private void addDocument(Vertex projectRevV, SourceDocument document) {
		Vertex documentV = graph.addVertex(nt(SourceDocument));
		SourceDocumentInfo info = 
			document.getSourceContentHandler().getSourceDocumentInfo();
		documentV.property("documentId", document.getID());
		documentV.property("author", info.getContentInfoSet().getAuthor());
		documentV.property("description", info.getContentInfoSet().getDescription());
		documentV.property("publisher", info.getContentInfoSet().getPublisher());
		documentV.property("title", info.getContentInfoSet().getTitle());
		documentV.property("checsum", info.getTechInfoSet().getChecksum());
		documentV.property("charset", info.getTechInfoSet().getCharset());
		documentV.property("fileOSType", info.getTechInfoSet().getFileOSType());
		documentV.property("fileType", info.getTechInfoSet().getFileType());
		documentV.property("mimeType", info.getTechInfoSet().getMimeType());
		documentV.property("locale", info.getIndexInfoSet().getLocale());
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
				
				for (Any posEntry : entry.getValue().asList()) {
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

	private void addTagset(Vertex projectRevV, TagsetDefinition tagset) {
		Vertex tagsetV = graph.addVertex(nt(Tagset));
		
		tagsetV.property("tagsetId", tagset.getUuid());
		tagsetV.property("name", tagset.getName());
		tagsetV.property("revisionHash", tagset.getRevisionHash());
		tagsetV.property("description", "");// TODO: 
		tagsetV.property("tagset", tagset);
		
		projectRevV.addEdge(rt(hasTagset), tagsetV);
		
		for (TagDefinition tag : tagset) {
			addTag(tagsetV, tag);
		}
	}

	private void addTag(Vertex tagsetV, TagDefinition tag) {
		Vertex tagV = graph.addVertex(nt(Tag));
		
		tagV.property("tagId", tag.getUuid());
		tagV.property("author", tag.getAuthor());
		tagV.property("color", tag.getColor());
		tagV.property("name", tag.getName());
		
		tagsetV.addEdge(rt(hasTag), tagV);
		
		for (PropertyDefinition propertyDef : tag.getUserDefinedPropertyDefinitions()) {
			addPropertyDefinition(tagV, propertyDef);
		}
		
	}

	private void addPropertyDefinition(Vertex tagV, PropertyDefinition propertyDef) {
		Vertex propertyDefV = graph.addVertex(nt(NodeType.Property));
		propertyDefV.property("uuid", propertyDef.getUuid());
		propertyDefV.property("name", propertyDef.getName());
		propertyDefV.property("values", propertyDef.getPossibleValueList());
		
		tagV.addEdge(rt(hasProperty), propertyDefV);
	}

	@Override
	public void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document,
			Path tokenizedSourceDocumentPath) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public int getSourceDocumentsCount(String rootRevisionHash) throws Exception {
		
		GraphTraversalSource g = graph.traversal();

		return g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.count().next().intValue();
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCollection(String rootRevisionHash, String collectionId, String name, String umcRevisionHash,
			SourceDocument document, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<UserMarkupCollectionReference> getCollectionReferences(String rootRevisionHash, int offset, int limit)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCollectionReferenceCount(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return 0;
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
	public int getTagsetsCount(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public UserMarkupCollection getCollection(String rootRevisionHash, TagLibrary tagLibrary,
			UserMarkupCollectionReference collectionReference) throws Exception {

		
		
		
		return null;
	}

	@Override
	public void addTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeProperties(String rootRevisionHash, String collectionId, String collectionRevisionHash,
			String propertyDefId) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(String rootRevisionHash, TagInstance tagInstance, Collection<Property> properties)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash, TagDefinition tagDefinition)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(String rootRevisionHash,
			PropertyDefinition propertyDefinition, TagLibrary tagLibrary) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds,
			String collectionRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProjectRevisionHash(String oldRootRevisionHash, String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCollectionRevisionHash(String rootRevisionHash, UserMarkupCollectionReference collectionReference)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCollection(String rootRevisionHash, UserMarkupCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeCollection(String rootRevisionHash, UserMarkupCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDocument(String rootRevisionHash, SourceDocument document, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
