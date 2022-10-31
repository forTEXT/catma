package de.catma.repository.git.resource.provider;

import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;

import java.io.IOException;
import java.io.InputStream;

public class BranchAwareSourceContentHandler implements SourceContentHandler {
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final String username;
	private final ProjectReference projectReference;
	private final String branch;
	private final SourceContentHandler delegateSourceContentHandler;

	public BranchAwareSourceContentHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			String username,
			ProjectReference projectReference, 
			String branch, 
			SourceContentHandler sourceContentHandler
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.username = username;
		this.projectReference = projectReference;
		this.branch = branch;
		this.delegateSourceContentHandler = sourceContentHandler;
	}

	public boolean isLoaded() {
		return delegateSourceContentHandler.isLoaded();
	}

	public void load(InputStream is) throws IOException {
		delegateSourceContentHandler.load(is);
	}

	public void load() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.checkout(branch, false);

			delegateSourceContentHandler.load();

			localGitRepoManager.checkout(username, false);
		}
	}

	public void unload() {
		delegateSourceContentHandler.unload();
	}

	public boolean hasIntrinsicMarkupCollection() {
		return delegateSourceContentHandler.hasIntrinsicMarkupCollection();
	}

	public SourceDocumentInfo getSourceDocumentInfo() {
		return delegateSourceContentHandler.getSourceDocumentInfo();
	}
	public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		delegateSourceContentHandler.setSourceDocumentInfo(sourceDocumentInfo);
	}

	public String getContent() throws IOException {
		return delegateSourceContentHandler.getContent();
	}
}
