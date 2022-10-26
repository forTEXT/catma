package de.catma.repository.git.managers;

import javax.annotation.Nullable;

public class CreateRepositoryResponse {
	public String groupPath;
	public long repositoryId;
	public String repositoryHttpUrl;

	public CreateRepositoryResponse(@Nullable String groupPath, long repositoryId,
									String repositoryHttpUrl) {
		this.groupPath = groupPath;
		this.repositoryId = repositoryId;
		this.repositoryHttpUrl = repositoryHttpUrl;
	}
}