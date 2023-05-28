package de.catma.repository.git.resource.provider;

import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;

import java.io.IOException;

public class BranchAwareStandardContentHandler extends StandardContentHandler {
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final ProjectReference projectReference;
	private final String branchName;
	private final String currentUserBranchName;

	public BranchAwareStandardContentHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			ProjectReference projectReference,
			String branchName,
			String currentUserBranchName
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectReference = projectReference;
		this.branchName = branchName;
		this.currentUserBranchName = currentUserBranchName;
	}

	public void load() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.checkout(branchName, false);

			super.load();

			localGitRepoManager.checkout(currentUserBranchName, false);
		}
	}
}
