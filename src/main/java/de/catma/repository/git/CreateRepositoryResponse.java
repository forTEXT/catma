package de.catma.repository.git;

public class CreateRepositoryResponse {
	public String groupPath;
	public long repositoryId;
	public String repositoryHttpUrl;

	public CreateRepositoryResponse(String groupPath, long repositoryId,
									String repositoryHttpUrl) {
		this.groupPath = groupPath;
		this.repositoryId = repositoryId;
		this.repositoryHttpUrl = repositoryHttpUrl;
	}
}