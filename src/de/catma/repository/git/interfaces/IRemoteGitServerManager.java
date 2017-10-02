package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.RemoteGitServerManagerException;

import javax.annotation.Nullable;
import java.util.List;

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

	String createImpersonationToken(int userId, String tokenName)
			throws RemoteGitServerManagerException;
}
