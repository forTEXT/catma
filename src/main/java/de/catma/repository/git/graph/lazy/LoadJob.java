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
import de.catma.util.Pair;

import java.util.Map;
import java.util.logging.Logger;

class LoadJob extends DefaultProgressCallable<Pair<TagManager, Map<String, SourceDocumentReference>>> {
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
	public Pair<TagManager, Map<String, SourceDocumentReference>> call() throws Exception {
		Map<String, SourceDocumentReference> docRefsById = Maps.newHashMap();

		getProgressListener().setProgress("Loading tagsets for project \"%s\"", projectReference.getName());
		tagManager.load(tagsetsProvider.getTagsets());

		getProgressListener().setProgress(String.format("Loading documents for project \"%s\"", projectReference.getName()));
		for (SourceDocument document : documentsProvider.getDocuments()) {
			document.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().setURI(
					documentFileURIProvider.getDocumentFileURI(document.getUuid())
			);
			docRefsById.put(document.getUuid(), new SourceDocumentReference(document.getUuid(), document.getSourceContentHandler()));
		}

		getProgressListener().setProgress(String.format("Loading collections for project \"%s\"", projectReference.getName()));
		for (AnnotationCollection collection : collectionsProvider.getCollections(tagManager.getTagLibrary())) {
			docRefsById.get(collection.getSourceDocumentId()).addUserMarkupCollectionReference(new AnnotationCollectionReference(collection));
		}

		Logger.getLogger(getClass().getName()).info(
				String.format("Finished loading project \"%s\"", projectReference.getName())
		);

		return new Pair<>(tagManager, docRefsById);
	}
}
