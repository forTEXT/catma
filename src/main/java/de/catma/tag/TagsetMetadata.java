package de.catma.tag;

public final class TagsetMetadata {
	private String name;
	private String description;
	private String responsableUser;
	public TagsetMetadata(String name, String description, String responsableUser) {
		super();
		this.name = name;
		this.description = description;
		this.responsableUser = responsableUser;
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
	public String getResponsableUser() {
		return responsableUser;
	}
	public void setResponsableUser(String responsableUser) {
		this.responsableUser = responsableUser;
	}
}