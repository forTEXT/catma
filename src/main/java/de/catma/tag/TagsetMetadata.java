package de.catma.tag;

public final class TagsetMetadata {
	private String name;
	private String description;
	private String responsibleUser;
	public TagsetMetadata(String name, String description, String responsibleUser) {
		super();
		this.name = name;
		this.description = description;
		this.responsibleUser = responsibleUser;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
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
}