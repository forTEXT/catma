package de.catma.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;

public class ProjectReference {
	@SerializedName("id")
	private final String projectId;
	private final String namespace;
	private String name;
	private String description;
	private final ZonedDateTime createdAt;
	private final ZonedDateTime lastActivityAt;
	
	public ProjectReference(
			String projectId, String namespace, String name, String description) {
		this(projectId, namespace, name, description, null, null);
	}

	
	
	public ProjectReference(String projectId, String namespace, String name, String description, ZonedDateTime createdAt,
			ZonedDateTime lastActivityAt) {
		super();
		this.projectId = projectId; // TODO: what we are calling ID here is actually the path
		this.namespace = namespace;
		this.name = name;
		this.description = description;
		this.createdAt = createdAt;
		this.lastActivityAt = lastActivityAt;
	}

	@JsonProperty("id")
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

	@JsonIgnore
	public String getFullPath() {
		return String.format("%s/%s", namespace, projectId);
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}
	
	public ZonedDateTime getLastActivityAt() {
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
