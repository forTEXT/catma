package de.catma.ui.module.project;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Set;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

import de.catma.rbac.RBACRole;
import de.catma.util.DammAlgorithm;

/**
 * Project invitation bean class. It can be marshalled to json and back.
 * @author db
 *
 */
public class ProjectInvitation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6455694878222463900L;
	
	private final String projectId;
	private final String name;
	private final String description;
	private final int defaultRole;
	private final int key;
	private final boolean createOwnCollection;
	private final Set<String> resources;
	
	public static int generate(){
		return DammAlgorithm.padChecksum(new SecureRandom().nextInt(99999));
	}
	
	public ProjectInvitation(
			@JsonProperty("projectId") String projectId, 
			@JsonProperty("defaultRole") int defaultRole,
			@JsonProperty("name") String name,
			@JsonProperty("desfription") String description,
			@JsonProperty("createowncollection") boolean createOwnCollection,
			@JsonProperty("resources") Set<String> resources
			){
		this(projectId, defaultRole, name, description, createOwnCollection,resources, generate());
	}
	
	@JsonCreator
	public ProjectInvitation(
			@JsonProperty("projectId") String projectId, 
			@JsonProperty("defaultRole") int defaultRole,
			@JsonProperty("name") String name,
			@JsonProperty("desfription") String description,
			@JsonProperty("createowncollection") boolean createOwnCollection,
			@JsonProperty("resources") Set<String> resources,
			@JsonProperty("key") int key) {
		this.projectId = projectId;
		this.defaultRole = defaultRole;
		this.name = name;
		this.description = description;
		this.key = key;
		this.createOwnCollection = createOwnCollection;
		this.resources = resources;
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

	public boolean isCreateOwnCollection() {
		return createOwnCollection;
	}

	public Set<String> getResources() {
		return resources;
	}

	@Override
	public String toString() {
		return "ProjectInvitation [projectId=" + projectId + ", role=" + RBACRole.forValue(defaultRole) + "]";
	}

	
}
