package de.catma.rbac;

import java.io.IOException;

import de.catma.document.source.SourceDocument;
import de.catma.interfaces.IdentifiableResource;
import de.catma.models.Project;
import de.catma.tag.TagsetDefinition;

/**
 * Manages all access controls
 * 
 * @author db
 *
 */
public interface IRBACManager {
	
	/**
	 * Authorized a <code>RBACPermission</code> on <code>RBACSubject</code> for a {@link Project}
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
	boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, IdentifiableResource resource);

	/**
	 * assigns given role to subject in a given context here a {@link Project}
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
	RBACSubject assignOnResource(RBACSubject subject, RBACRole role, IdentifiableResource resource) throws IOException;

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
	void unassignFromResource(RBACSubject subject, IdentifiableResource resource) throws IOException;
	
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
			return role.value >= permission.getRoleRequired().value;
		}
	};
	
}
