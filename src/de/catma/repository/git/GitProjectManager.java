package de.catma.repository.git;

import java.util.Map;

import de.catma.Pager;
import de.catma.document.repository.Repository;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.User;

public class GitProjectManager implements ProjectManager {
	
	private GitUser user;
	private IRemoteGitServerManager gitLabServerManager;
	private String gitBasedRepositoryBasePath;

	public GitProjectManager(
			String gitLabServerUrl, 
			String gitLabAdminPersonalAccessToken,
			String gitBasedRepositoryBasePath,
			Map<String, String>	userIdentification) 
					throws RemoteGitServerManagerException {
		this.gitBasedRepositoryBasePath = gitBasedRepositoryBasePath;
		this.gitLabServerManager = 
				new GitLabServerManager(
					gitLabAdminPersonalAccessToken, 
					gitLabAdminPersonalAccessToken, 
					userIdentification);
		this.user = new GitUser(((GitLabServerManager) this.gitLabServerManager).getGitLabUser());
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public Pager<ProjectReference> getProjectReferences() throws Exception {
		return gitLabServerManager.getProjectReferences();
	}

	@Override
	public ProjectReference createProject(String name, String description) throws Exception {
		try (JGitRepoManager jGitRepoManager = 
				new JGitRepoManager(this.gitBasedRepositoryBasePath, this.user)) {

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(name, description);
			
			return new ProjectReference(projectId, name, description);
		}
	}

	@Override
	public Repository openProject(ProjectReference projectReference) {
		// TODO Auto-generated method stub
		return null;
	}

}
