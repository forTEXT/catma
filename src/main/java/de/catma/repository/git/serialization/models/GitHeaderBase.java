package de.catma.repository.git.serialization.models;

public class GitHeaderBase {
	private String name;
	private String description;
	private String responsableUser;
	private String forkedFromCommitURL;

	public GitHeaderBase(){}

	public GitHeaderBase(
			String name, String description, 
			String responsableUser, String forkedFromCommitURL) {
		this.name = name;
		this.description = description;
		this.responsableUser = responsableUser;
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
	
	public String getResponsableUser() {
		return responsableUser;
	}
	
	public void setResponsableUser(String responsableUser) {
		this.responsableUser = responsableUser;
	}
	
	public String getForkedFromCommitURL() {
		return forkedFromCommitURL;
	}
	
	public void setForkedFromCommitURL(String forkedFromCommitURL) {
		this.forkedFromCommitURL = forkedFromCommitURL;
	}
}
