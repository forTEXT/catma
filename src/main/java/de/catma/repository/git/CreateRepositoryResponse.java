package de.catma.repository.git;

import javax.annotation.Nullable;

public class CreateRepositoryResponse {
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