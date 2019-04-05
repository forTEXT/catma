package de.catma.ui.modules.project;

import java.security.SecureRandom;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

import de.catma.DammAlgorithm;
import de.catma.rbac.RBACRole;

/**
 * Project invitation bean class. It can be marshalled to json and back.
 * @author db
 *
 */
public class ProjectInvitation {

	private final String projectId;
	private final String name;
	private final String description;
	private final int defaultRole;
	private final int key;
	
	public static int generate(){
		return DammAlgorithm.padChecksum(new SecureRandom().nextInt(99999));
	}
	
	public ProjectInvitation(
			@JsonProperty("projectId") String projectId, 
			@JsonProperty("defaultRole") int defaultRole,
			@JsonProperty("name") String name,
			@JsonProperty("desfription") String description){
		this(projectId, defaultRole, name, description, generate());
	}
	
	@JsonCreator
	public ProjectInvitation(
			@JsonProperty("projectId") String projectId, 
			@JsonProperty("defaultRole") int defaultRole,
			@JsonProperty("name") String name,
			@JsonProperty("desfription") String description,
			@JsonProperty("key") int key) {
		this.projectId = projectId;
		this.defaultRole = defaultRole;
		this.name = name;
		this.description = description;
		this.key = key;
	}

	public String getProjectId() {
		return projectId;
	}

	public int getDefaultRole() {
		return defaultRole;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getName() {
		return name;
	}

	public int getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "ProjectInvitation [projectId=" + projectId + ", role=" + RBACRole.forValue(defaultRole) + "]";
	}

	
}
