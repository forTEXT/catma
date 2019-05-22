package de.catma.project;

public class ProjectReference {
	
	private String projectId;
	private String name;
	private String description;
	
	public ProjectReference(String projectId, String name, String description) {
		super();
		this.projectId = projectId;
		this.name = name;
		this.description = description;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return "Project " + name + " #" + projectId;
	}
	
}
