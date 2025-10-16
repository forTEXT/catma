package de.catma.rbac;

import de.catma.project.ProjectReference;
import de.catma.user.Group;
import de.catma.user.SharedGroup;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Role-Based Access Control Manager
 *
 * @see de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon
 */
public interface RBACManager {
	default boolean hasPermission(RBACRole role, RBACPermission permission) {
		if (role == null || permission == null) {
			return false;
		}

		return role.getAccessLevel() >= permission.getRoleRequired().getAccessLevel();
	}

	RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException;

	RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException;

	boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, ProjectReference projectReference);


	// TODO: consider renaming 'assign' functions - rather use the same language as GitLab (also see callers)
	RBACSubject assignOnGroup(RBACSubject subject, Long groupId, LocalDate expiresAt) throws IOException;

	RBACSubject updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException;

	void unassignFromGroup(RBACSubject subject, Long groupId) throws IOException;


	RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException;

	void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException;


	SharedGroup assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign)
			throws IOException;

	void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException;
}
