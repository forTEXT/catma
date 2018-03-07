package de.catma.repository.git.interfaces;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import de.catma.Pager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;

public interface IRemoteGitServerManager {
	class CreateRepositoryResponse {
		public String groupPath;
		public int repositoryId;
		public String repositoryHttpUrl;

		public CreateRepositoryResponse(@Nullable String groupPath, int repositoryId,
										String repositoryHttpUrl) {
			this.groupPath = groupPath;
			this.repositoryId = repositoryId;
			this.repositoryHttpUrl = repositoryHttpUrl;
		}
	}

	CreateRepositoryResponse createRepository(String name, @Nullable String path)
			throws RemoteGitServerManagerException;

	CreateRepositoryResponse createRepository(String name, @Nullable String path, String groupPath)
			throws RemoteGitServerManagerException;

	void deleteRepository(int repositoryId) throws RemoteGitServerManagerException;

	String createGroup(String name, String path, @Nullable String description)
			throws RemoteGitServerManagerException;

	List<String> getGroupRepositoryNames(String path) throws RemoteGitServerManagerException;

	void deleteGroup(String path) throws RemoteGitServerManagerException;

	int createUser(String email, String username, @Nullable String password,
				   String name, @Nullable Boolean isAdmin)
			throws RemoteGitServerManagerException;
	
	Pager<ProjectReference> getProjectReferences() throws RemoteGitServerManagerException;
	
	String getUsername();
	String getPassword();

	String getEmail();

	Set<String> getProjectRepositoryUrls(ProjectReference projectReference) throws RemoteGitServerManagerException;

	String getProjectRootRepositoryUrl(ProjectReference projectReference) throws RemoteGitServerManagerException;
}
