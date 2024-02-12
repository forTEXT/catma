package de.catma.rbac;

import java.io.IOException;
import java.time.LocalDate;

import de.catma.project.ProjectReference;
import de.catma.user.Group;
import de.catma.user.SharedGroup;

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
	boolean isAuthorizedOnProject(
			RBACSubject subject, RBACPermission permission, ProjectReference projectReference);

	/**
	 * assigns given role to subject in a given context here a CATMA project}
	 * 
	 * @param subject
	 * @param role
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException;

	/**
	 * unassigns a subject from a project
	 * @param subject
	 * @param projectId
	 * @throws IOException
	 */
	void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException;

	
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
	 * gets the defined Role for a subject on a specific project
	 * 
	 * @param subject
	 * @param projectReference
	 * @return
	 * @throws IOException 
	 */
	RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException;

	/**
	 * gets the defined Role for a subject on a specific group
	 * @param subject
	 * @param group
	 * @return
	 * @throws IOException
	 */
	RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException;

	
	/**
	 * Assign assistant role to the given subject in the context of the given group.
	 * @param subject 
	 * @param groupId
	 * @return
	 * @throws IOException
	 */
	RBACSubject assignOnGroup(RBACSubject subject, Long groupId, LocalDate expiresAt) throws IOException;
	
	/**
	 * Assign the given role to the given group in the context of the given project.
	 * @param sharedGroup
	 * @param projectReference
	 * @return
	 * @throws IOException
	 */
	SharedGroup assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign) throws IOException;


	
	/**
	 * Unassign the given group from the given project.
	 * @param sharedGroup
	 * @param projectReference
	 * @throws IOException
	 */
	public void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException;
	
}
