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

	// TODO: this is only used in one place - hasPermission is used everywhere else (consistency; it would probably be cleaner to perform all permission checks
	//       on the basis of resource/subject/permission, rather than relying on callers to do some of the work upfront, as is the case with hasPermission)
	boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, ProjectReference projectReference);


	// TODO: consider renaming 'assign' functions - rather use the same language as GitLab (also see callers)
	RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException;

	void assignOnGroup(RBACSubject subject, Long groupId, LocalDate expiresAt) throws IOException;

	void updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException;

	void unassignFromGroup(RBACSubject subject, Long groupId) throws IOException;


	RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException;

	// TODO: dual add/update implementation, consider splitting up
	void assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException;

	void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException;


	// TODO: dual add/update implementation, consider splitting up
	void assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign) throws IOException;

	void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException;
}
