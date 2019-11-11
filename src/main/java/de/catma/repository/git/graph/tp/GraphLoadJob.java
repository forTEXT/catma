package de.catma.repository.git.graph.tp;

import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.rt;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.repository.git.graph.GraphProjectHandler.CollectionsSupplier;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.Pair;

public class GraphLoadJob extends DefaultProgressCallable<Pair<TagManager, Graph>> {

	private Logger logger = Logger.getLogger(TPGraphProjectHandler.class.getName());
	
	private final Graph graph;
	private final ProjectReference projectReference;
	private final User user;
	private final String revisionHash;
	private final Supplier<List<TagsetDefinition>> tagsetsSupplier; 
	private final Supplier<List<SourceDocument>> documentsSupplier;
	private final CollectionsSupplier collectionsSupplier;
	private final GraphWriter graphWriter;
	private final TagManager tagManager;

	public GraphLoadJob(Graph graph, ProjectReference projectReference, TagManager tagManager, User user, String revisionHash,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier, FileInfoProvider fileInfoProvider) {
		super();
		this.graph = graph;
		this.projectReference = projectReference;
		this.tagManager = tagManager;
		this.user = user;
		this.revisionHash = revisionHash;
		this.tagsetsSupplier = tagsetsSupplier;
		this.documentsSupplier = documentsSupplier;
		this.collectionsSupplier = collectionsSupplier;
		this.graphWriter = new GraphWriter(graph, fileInfoProvider, projectReference, user);
	}

	@Override
	public Pair<TagManager, Graph> call() throws Exception {

		getProgressListener().setProgress("Start loading project %1$s...", projectReference.getName());

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
		getProgressListener().setProgress("Loading Tagsets...");
		tagManager.load(tagsets);
		for (TagsetDefinition tagset : tagsets) {
			getProgressListener().setProgress("Indexing Tagset %1$s...", tagset.getName());
			Vertex tagsetV = graphWriter.addTagset(projectRevV, tagset);
			graphWriter.addHasParentRelations(tagsetV, tagset);
		}
		
		for (SourceDocument document : documentsSupplier.get()) {
			getProgressListener().setProgress("Indexing Document %1$s...", document.toString());
			graphWriter.addDocument(projectRevV, document);
		}

		for (AnnotationCollection collection : collectionsSupplier.get(tagManager.getTagLibrary())) {
			getProgressListener().setProgress("Indexing Collection %1$s...", collection.toString());

			graphWriter.addCollection(revisionHash, revisionHash, collection);
		}
		getProgressListener().setProgress("Finished loading project %1$s", projectReference.getName());

		logger.info("Finished loading " + projectReference.getName() + " " + projectReference.getProjectId());
		
		return new Pair<>(tagManager, graph);
	}

}
