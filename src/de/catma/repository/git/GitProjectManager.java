package de.catma.repository.git;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.catma.document.repository.Repository;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.user.User;

public class GitProjectManager implements ProjectManager {
	
	private GitUser user;
	private IRemoteGitServerManager gitLabServerManager;

	public GitProjectManager(Properties properties, Map<String, String> userIdentification) throws RemoteGitServerManagerException {
		this.gitLabServerManager = new GitLabServerManager(properties, userIdentification);
		this.user = new GitUser(((GitLabServerManager) this.gitLabServerManager).getGitLabUser());
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws Exception {
		return gitLabServerManager.getProjectReferences();
	}

	@Override
	public ProjectReference createProject(String name, String description) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Repository openProject(ProjectReference projectReference) {
		// TODO Auto-generated method stub
		return null;
	}

}
