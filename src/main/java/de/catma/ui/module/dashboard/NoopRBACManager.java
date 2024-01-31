package de.catma.ui.module.dashboard;

import java.io.IOException;

import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.user.Group;

public class NoopRBACManager implements IRBACManager {

	@Override
	public boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, ProjectReference projectReference) {
		return false;
	}

	@Override
	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference) throws IOException {
		throw new UnsupportedOperationException("Operation not supported");
	}

	@Override
	public void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		throw new UnsupportedOperationException("Operation not supported");
	}

	@Override
	public RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		return RBACRole.NONE;
	}

	@Override
	public RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException {
		return RBACRole.NONE;
	}

	@Override
	public RBACSubject assignOnGroup(RBACSubject subject, Long groupId) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	

}
