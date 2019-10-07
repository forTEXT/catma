package de.catma.rbac;

import java.io.IOException;

import de.catma.document.source.SourceDocument;
import de.catma.tag.TagsetDefinition;

/**
 * Manages all access controls
 * 
 * @author db
 *
 */
public interface IRBACManager {
	
	/**
	 * Authorized a <code>RBACPermission</code> on <code>RBACSubject</code> for a projectId
	 *
	 * @param subject
	 * @param permission
	 * @param projectId
	 * @return
	 */
	boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, String projectId);

	/**
	 * Authorized a <code>RBACPermission</code> on <code>RBACSubject</code> for a resource e.g. 
	 * {@link SourceDocument}, {@link TagsetDefinition} and so on.
	 *
	 * @param subject
	 * @param permission
	 * @param projectId
	 * @return
	 */
	boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, String projectId, String resourceId);

	/**
	 * assigns given role to subject in a given context here a projectId}
	 * 
	 * @param subject
	 * @param role
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	RBACSubject assignOnProject(RBACSubject subject, RBACRole role, String projectId) throws IOException;

	/**
	 * assigns given role to subject in a given context here a resource like {@link SourceDocument}, {@link TagsetDefinition} etc...
	 * @param subject
	 * @param role
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	RBACSubject assignOnResource(RBACSubject subject, RBACRole role, String projectId, String resourceId) throws IOException;

	/**
	 * unassigns a subject from a project
	 * @param subject
	 * @param projectId
	 * @throws IOException
	 */
	void unassignFromProject(RBACSubject subject, String projectId) throws IOException;
	
	/**
	 * unassigns a subject from a resource
	 * @param subject
	 * @param projectId
	 * @throws IOException
	 */
	void unassignFromResource(RBACSubject subject, String projectId, String resourceId) throws IOException;
	
	/**
	 * checks if a role has a permission assigned
	 * @param role
	 * @param permission
	 * @return
	 */
	default boolean hasPermission(RBACRole role, RBACPermission permission) {
		if(role == null || permission == null) {
			return false;
		} else {
			return role.getAccessLevel() >= permission.getRoleRequired().getAccessLevel();
		}
	};
	
	/**
	 * get's the defined Role for a subject on a specific resource
	 * 
	 * @param subject
	 * @param resource
	 * @return
	 * @throws IOException 
	 */
	RBACRole getRoleOnResource(RBACSubject subject, String projectId, String resourceId) throws IOException;
	
	/**
	 * get's the defined Role for a subject on a specific project
	 * 
	 * @param subject
	 * @param projectId
	 * @return
	 * @throws IOException 
	 */
	RBACRole getRoleOnProject(RBACSubject subject, String projectId) throws IOException;

	
}
