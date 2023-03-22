package de.catma.repository.git.resource.provider;

import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;

import java.io.IOException;

public class BranchAwareSourceContentHandler extends StandardContentHandler {
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final String username;
	private final ProjectReference projectReference;
	private final String branch;

	public BranchAwareSourceContentHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			String username,
			ProjectReference projectReference,
			String branch
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.username = username;
		this.projectReference = projectReference;
		this.branch = branch;
	}

	public void load() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.checkout(branch, false);

			super.load();

			localGitRepoManager.checkout(username, false);
		}
	}
}
