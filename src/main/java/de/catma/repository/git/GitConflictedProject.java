package de.catma.repository.git;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import de.catma.document.source.SourceDocument;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.ConflictedProject;
import de.catma.tag.TagsetDefinition;

public class GitConflictedProject implements ConflictedProject {
	
	private GitProjectHandler gitProjectHandler;
	private Function<String, URI> documentIdTofileUriMapper;
	
	public GitConflictedProject(GitProjectHandler gitProjectHandler, Function<String, URI> documentIdTofileUriMapper) {
		super();
		this.gitProjectHandler = gitProjectHandler;
		this.documentIdTofileUriMapper = documentIdTofileUriMapper;
	}

	@Override
	public List<CollectionConflict> getCollectionConflicts() throws Exception {
		return gitProjectHandler.getCollectionConflicts();
	}

	@Override
	public Collection<TagsetDefinition> getTagsets() throws Exception {
		return gitProjectHandler.getTagsets();
	}

	@Override
	public Collection<SourceDocument> getDocuments() throws Exception {
		Collection<SourceDocument> documents = gitProjectHandler.getDocuments();
		documents.forEach(
			doc -> 
				doc
				.getSourceContentHandler()
				.getSourceDocumentInfo()
				.getTechInfoSet()
				.setURI(documentIdTofileUriMapper.apply(doc.getID())));
		
		return documents;
	}
}
