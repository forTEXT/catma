package de.catma.repository.git;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import de.catma.Pager;
import de.catma.document.repository.Repository;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.User;

public class GitProjectManager implements ProjectManager {
	
	private GitUser user;
	private IRemoteGitServerManager remoteGitServerManager;
	private String gitBasedRepositoryBasePath;
	private ILocalGitRepositoryManager localGitRepositoryManager;

	public GitProjectManager(
			String gitLabServerUrl, 
			String gitLabAdminPersonalAccessToken,
			String gitBasedRepositoryBasePath,
			Map<String, String>	userIdentification) 
					throws RemoteGitServerManagerException {
		this.gitBasedRepositoryBasePath = gitBasedRepositoryBasePath;
		this.remoteGitServerManager = 
				new GitLabServerManager(
					gitLabAdminPersonalAccessToken, 
					gitLabAdminPersonalAccessToken, 
					userIdentification);
		this.user = new GitUser(((GitLabServerManager) this.remoteGitServerManager).getGitLabUser());
		this.localGitRepositoryManager = new JGitRepoManager(this.gitBasedRepositoryBasePath, this.user);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public Pager<ProjectReference> getProjectReferences() throws Exception {
		return remoteGitServerManager.getProjectReferences();
	}

	@Override
	public ProjectReference createProject(String name, String description) throws Exception {
		GitProjectHandler gitProjectHandler = 
				new GitProjectHandler(this.localGitRepositoryManager, this.remoteGitServerManager);

		String projectId = gitProjectHandler.create(name, description);
		
		return new ProjectReference(projectId, name, description);
	}

	@Override
	public Repository openProject(ProjectReference projectReference) {
		
		return null;
	}

	public boolean existsLocally(ProjectReference projectReference) {
		return Paths.get(new File(this.gitBasedRepositoryBasePath).toURI())
				.resolve(projectReference.getProjectId())
				.toFile()
				.exists();
	}
	
	public String cloneLocally(ProjectReference projectReference) {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			// clone the root repository locally
			return localRepoManager.clone(
				projectReference.getProjectId(),
				response.repositoryHttpUrl,
				null,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
			);
		}
	}
}
