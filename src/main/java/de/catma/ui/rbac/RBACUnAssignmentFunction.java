package de.catma.ui.rbac;

import java.io.IOException;

import de.catma.rbac.RBACSubject;

//	private final String projectId;
public interface RBACUnAssignmentFunction<D> {
	void unassign(RBACSubject subject, D resourceOrProject) throws IOException;
}