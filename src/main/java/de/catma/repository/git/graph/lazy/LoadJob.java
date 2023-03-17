package de.catma.repository.git.graph.lazy;

import com.google.common.collect.Maps;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.interfaces.CollectionsProvider;
import de.catma.repository.git.graph.interfaces.DocumentFileURIProvider;
import de.catma.repository.git.graph.interfaces.DocumentsProvider;
import de.catma.repository.git.graph.interfaces.TagsetsProvider;
import de.catma.tag.TagManager;

import java.util.Map;

class LoadJob extends DefaultProgressCallable<Map<String, SourceDocumentReference>> {
	private final ProjectReference projectReference;
	private final String revisionHash;
	private final TagManager tagManager;
	private final TagsetsProvider tagsetsProvider;
	private final DocumentsProvider documentsProvider;
	private final CollectionsProvider collectionsProvider;
	private final DocumentFileURIProvider documentFileURIProvider;

	public LoadJob(
			ProjectReference projectReference,
			String revisionHash,
			TagManager tagManager,
			TagsetsProvider tagsetsProvider,
			DocumentsProvider documentsProvider,
			CollectionsProvider collectionsProvider,
			DocumentFileURIProvider documentFileURIProvider
	) {
		this.projectReference = projectReference;
		this.revisionHash = revisionHash;
		this.tagManager = tagManager;
		this.tagsetsProvider = tagsetsProvider;
		this.documentsProvider = documentsProvider;
		this.collectionsProvider = collectionsProvider;
		this.documentFileURIProvider = documentFileURIProvider;
	}

	@Override
	public Map<String, SourceDocumentReference> call() throws Exception {
		Map<String, SourceDocumentReference> docRefsById = Maps.newHashMap();

		getProgressListener().setProgress(
				"Loading tagsets for project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId()
		);
		tagManager.load(tagsetsProvider.getTagsets());

		getProgressListener().setProgress(
				"Loading documents for project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId()
		);
		for (SourceDocument document : documentsProvider.getDocuments()) {
			// TODO: see TODOs in GitProjectHandler.createSourceDocument
			document.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().setURI(
					documentFileURIProvider.getDocumentFileURI(document.getUuid())
			);
			docRefsById.put(document.getUuid(), new SourceDocumentReference(document.getUuid(), document.getSourceContentHandler()));
		}

		getProgressListener().setProgress(
				"Loading collections for project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId()
		);
		for (AnnotationCollection collection : collectionsProvider.getCollections(tagManager.getTagLibrary())) {
			docRefsById.get(collection.getSourceDocumentId()).addUserMarkupCollectionReference(new AnnotationCollectionReference(collection));
		}

		return docRefsById;
	}
}
