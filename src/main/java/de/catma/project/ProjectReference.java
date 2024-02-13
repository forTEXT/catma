package de.catma.project;

import java.time.LocalDate;

public class ProjectReference {
	
	private final String projectId;
	private final String namespace;
	private String name;
	private String description;
	private final LocalDate createdAt;
	private final LocalDate lastActivityAt;
	
	public ProjectReference(
			String projectId, String namespace, String name, String description) {
		this(projectId, namespace, name, description, null, null);
	}

	
	
	public ProjectReference(String projectId, String namespace, String name, String description, LocalDate createdAt,
			LocalDate lastActivityAt) {
		super();
		this.projectId = projectId;
		this.namespace = namespace;
		this.name = name;
		this.description = description;
		this.createdAt = createdAt;
		this.lastActivityAt = lastActivityAt;
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
		return (name == null || name.isEmpty()) ? "#" + namespace + "/" + projectId : name;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getFullPath() {
		return String.format("%s/%s", namespace, projectId);
	}

	public LocalDate getCreatedAt() {
		return createdAt;
	}
	
	public LocalDate getLastActivityAt() {
		return lastActivityAt;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ProjectReference))
			return false;
		ProjectReference other = (ProjectReference) obj;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		return true;
	}
	
}
