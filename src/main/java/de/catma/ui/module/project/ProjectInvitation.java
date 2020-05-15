package de.catma.ui.module.project;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Set;

import de.catma.rbac.RBACRole;
import de.catma.util.DammAlgorithm;

/**
 * Project invitation bean class. It can be marshalled to json and back.
 * @author db
 *
 */
public final class ProjectInvitation implements Serializable {

	private static final long serialVersionUID = 6455694878222463900L;
	
	private String projectId;
	private String name;
	private String description;
	private int defaultRole;
	private boolean createOwnCollection;
	private Set<String> documentIds;
	private int key;
	
	private static final int generateKey(){
		return DammAlgorithm.padChecksum(new SecureRandom().nextInt(99999));
	}
	
	public ProjectInvitation() {
	}
	
	public ProjectInvitation(
			String projectId, int defaultRole, 
			String name, String description, 
			boolean createOwnCollection, Set<String> documentIds) {
		
		this(projectId, defaultRole, name, description, createOwnCollection,documentIds, generateKey());
		
	}
	
	public ProjectInvitation(
			String projectId, 
			int defaultRole,
			String name,
			String description,
			boolean createOwnCollection,
			Set<String> documentIds,
			int key) {
		
		this.projectId = projectId;
		this.defaultRole = defaultRole;
		this.name = name;
		this.description = description;
		this.key = key;
		this.createOwnCollection = createOwnCollection;
		this.documentIds = documentIds;
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

	// needs to start with get... for json serialization 
	public boolean getCreateOwnCollection() {
		return createOwnCollection;
	}
	
	public Set<String> getDocumentIds() {
		return documentIds;
	}

	@Override
	public String toString() {
		return "ProjectInvitation [projectId=" + projectId + ", role=" + RBACRole.forValue(defaultRole) + "]";
	}

	
}
