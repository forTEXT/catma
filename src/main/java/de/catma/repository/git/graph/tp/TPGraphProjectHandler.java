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
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
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

	private Graph graph;
	private ProjectReference projectReference;
	private User user;
	private FileInfoProvider fileInfoProvider;
	private TagManager tagManager;
	private GraphWriter graphWriter;
	
	public TPGraphProjectHandler(ProjectReference projectReference, 
			User user, FileInfoProvider fileInfoProvider) {
		this.projectReference = projectReference;
		this.user = user;
		this.fileInfoProvider = fileInfoProvider;
		
		graph = TinkerGraph.open();
		this.graphWriter = new GraphWriter(graph, fileInfoProvider, projectReference, user);
	}
	
	@Override
	public void ensureProjectRevisionIsLoaded(
			ExecutionListener<Void> openProjectListener, 
			ProgressListener progressListener,
			String revisionHash, 
			TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, 
			Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier,
			BackgroundService backgroundService) throws Exception {
		
		GraphTraversalSource g = graph.traversal();

		if (!g.V().has(nt(ProjectRevision), "revisionHash", revisionHash).hasNext()) {
			((TinkerGraph)graph).clear();
			
			backgroundService.submit(
				new GraphLoadJob(
						graph, 
						projectReference, 
						tagManager,
						user, 
						revisionHash, 
						tagsetsSupplier, 
						documentsSupplier, 
						collectionsSupplier, 
						fileInfoProvider), 
				new ExecutionListener<TagManager>() {
					@Override
					public void done(TagManager result) {
						TPGraphProjectHandler.this.tagManager = result;
						openProjectListener.done(null);
					}
					@Override
					public void error(Throwable t) {
						openProjectListener.error(t);
					}
				}, 
				progressListener);
		}
	}



	@Override
	public void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document,
			Path tokenizedSourceDocumentPath) throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex projectRevV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash).next();
		projectRevV.property("revisionHash", rootRevisionHash);
		
		graphWriter.addDocument(projectRevV, document);
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
		graphWriter.addCollection(
			oldRootRevisionHash,
			rootRevisionHash,
			new AnnotationCollection(collectionId, new ContentInfoSet(name), tagManager.getTagLibrary(), 
					document.getUuid(), document.getRevisionHash()));
	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		GraphTraversalSource g = graph.traversal();

		Vertex v = g.V().hasLabel(nt(ProjectRevision)).next();
		System.out.println(v);
		System.out.println(v.property("revisionHash").value());
		Vertex projectRevV = 
			g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash).next();
		projectRevV.property("revisionHash", rootRevisionHash);
		
		Vertex tagsetV = graphWriter.addTagset(projectRevV, tagset);
		graphWriter.addHasParentRelations(tagsetV, tagset);
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
		
		graphWriter.addTag(tagsetV, parentTagV, tag);
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
		
		graphWriter.addPropertyDefinition(tagV, propertyDefinition);
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
	public AnnotationCollection getCollection(String rootRevisionHash, TagLibrary tagLibrary,
			AnnotationCollectionReference collectionReference) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		return 
			g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", 
				collectionReference.getSourceDocumentId())
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collectionReference.getId())
			.properties("collection")
			.map(prop -> (AnnotationCollection)prop.get().orElse(null))
			.next();
	}

	@Override
	public void addTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		System.out.println(graph);
		GraphTraversalSource g = graph.traversal();
		Vertex collectionV = 
			g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", 
				collection.getSourceDocumentId())
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", collection.getId())
			.next();

		graphWriter.addTagReferences(rootRevisionHash, collectionV, tagReferences);
		System.out.println(graph);
	}

	@Override
	public void removeTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		
		System.out.println(graph);
		
		Set<String> tagInstanceIds = 
			tagReferences
				.stream()
				.map(tr -> tr.getTagInstanceId())
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
			AnnotationCollection collection, 
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
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash, TagDefinition tag)
			throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		List<Map<String,Object>> resultMap = g.V().has(nt(ProjectRevision), "revisionHash", rootRevisionHash)
		.outE(rt(hasTagset)).inV().has(nt(Tagset), "tagsetId", tag.getTagsetDefinitionUuid())
		.outE(rt(hasTag)).inV().has(nt(Tag), "tagId", tag.getUuid())
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
		.as("annotationId")
		.inE(rt(hasInstance)).outV().hasLabel(nt(MarkupCollection))
		.as("collectionId")
		.select("annotationId", "collectionId")
		.by("tagInstanceId").by("collectionId")
		.toList();
		
		
		Multimap<String, String> result = HashMultimap.create();
		
		for (Map<String,Object> entry : resultMap) {
			result.put((String)entry.get("collectionId"), (String)entry.get("annotationId"));
		}
		
		return result;
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
		.map(prop -> (AnnotationCollection)prop.get().orElse(null))
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
	public void updateCollection(String rootRevisionHash, AnnotationCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();

		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.next()
		.property("revisionHash", rootRevisionHash);
		
		// the collection itself has currently no indexed fields so there is no updated to be done
	}

	@Override
	public void removeCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		GraphTraversalSource g = graph.traversal();
		
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
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
		
		g.V().has(nt(ProjectRevision), "revisionHash", oldRootRevisionHash)
		.property("revisionHash", rootRevisionHash)
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", document.getUuid())
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
