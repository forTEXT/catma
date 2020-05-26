package de.catma.ui.module.dashboard;

import java.io.IOException;

import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;

public class NoopRBACManager implements IRBACManager {

	@Override
	public boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, String projectId) {
		return false;
	}

	@Override
	public boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, String projectId,
			String resourceId) {
		return false;
	}

	@Override
	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role, String projectId) throws IOException {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public RBACSubject assignOnResource(RBACSubject subject, RBACRole role, String projectId, String resourceId)
			throws IOException {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public void unassignFromProject(RBACSubject subject, String projectId) throws IOException {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public void unassignFromResource(RBACSubject subject, String projectId, String resourceId) throws IOException {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public RBACRole getRoleOnResource(RBACSubject subject, String projectId, String resourceId) throws IOException {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public RBACRole getRoleOnProject(RBACSubject subject, String projectId) throws IOException {
		return RBACRole.NONE;
	}

}
