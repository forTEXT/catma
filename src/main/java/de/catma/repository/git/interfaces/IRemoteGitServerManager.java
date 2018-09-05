package de.catma.repository.git.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import de.catma.Pager;
import de.catma.project.ProjectReference;

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
			throws IOException;

	CreateRepositoryResponse createRepository(String name, @Nullable String path, String groupPath)
			throws IOException;

	void deleteRepository(int repositoryId) throws IOException;

	String createGroup(String name, String path, @Nullable String description)
			throws IOException;

	List<String> getGroupRepositoryNames(String path) throws IOException;

	void deleteGroup(String path) throws IOException;

	int createUser(String email, String username, @Nullable String password,
				   String name, @Nullable Boolean isAdmin)
			throws IOException;
	
	Pager<ProjectReference> getProjectReferences() throws IOException;
	
	String getUsername();
	String getPassword();

	String getEmail();

	Set<String> getProjectRepositoryUrls(ProjectReference projectReference) throws IOException;

	String getProjectRootRepositoryUrl(ProjectReference projectReference) throws IOException;
}
