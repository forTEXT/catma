package de.catma.rbac;

import java.io.IOException;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
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
	boolean isAuthorizedOnProject(
			RBACSubject subject, RBACPermission permission, ProjectReference projectReference);

	/**
	 * assigns given role to subject in a given context here a CATMA Project}
	 * 
	 * @param subject
	 * @param role
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference) throws IOException;

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
	 * get's the defined Role for a subject on a specific project
	 * 
	 * @param subject
	 * @param projectId
	 * @return
	 * @throws IOException 
	 */
	RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException;

	
}
