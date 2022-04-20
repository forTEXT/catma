package de.catma.repository.git.serialization.models;

public class GitHeaderBase {
	private String name;
	private String description;
	private String responsibleUser;
	private String forkedFromCommitURL;

	public GitHeaderBase(){}

	public GitHeaderBase(
			String name, String description, 
			String responsibleUser, String forkedFromCommitURL) {
		this.name = name;
		this.description = description;
		this.responsibleUser = responsibleUser;
		this.forkedFromCommitURL = forkedFromCommitURL;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getResponsibleUser() {
		return responsibleUser;
	}
	
	public void setResponsibleUser(String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}
	
	public String getForkedFromCommitURL() {
		return forkedFromCommitURL;
	}
	
	public void setForkedFromCommitURL(String forkedFromCommitURL) {
		this.forkedFromCommitURL = forkedFromCommitURL;
	}
}
