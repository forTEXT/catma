package de.catma.repository.git.resource.provider;

import java.io.IOException;
import java.io.InputStream;

import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;

public class BranchAwareSourceContentHandler implements SourceContentHandler {

	private LocalGitRepositoryManager localGitRepositoryManager;
	private String username;
	private ProjectReference projectReference;
	private String branch;
	private SourceContentHandler delegate;

	public BranchAwareSourceContentHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			String username,
			ProjectReference projectReference, 
			String branch, 
			SourceContentHandler sourceContentHandler) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.username = username;
		this.projectReference = projectReference;
		this.branch = branch;
		this.delegate = sourceContentHandler;
	}

	public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		delegate.setSourceDocumentInfo(sourceDocumentInfo);
	}

	public SourceDocumentInfo getSourceDocumentInfo() {
		return delegate.getSourceDocumentInfo();
	}

	public void load(InputStream is) throws IOException {
		delegate.load(is);
	}

	public void load() throws IOException {
		try (LocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			localGitRepManager.checkout(branch, false);

			delegate.load();
			
			localGitRepManager.checkout(username, false);

		}
	}

	public String getContent() throws IOException {
		return delegate.getContent();
	}

	public void unload() {
		delegate.unload();
	}

	public boolean isLoaded() {
		return delegate.isLoaded();
	}

	public boolean hasIntrinsicMarkupCollection() {
		return delegate.hasIntrinsicMarkupCollection();
	}
	
	

}
