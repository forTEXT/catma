package de.catma.ui.rbac;

import java.io.IOException;

import de.catma.rbac.RBACSubject;

public interface RBACUnAssignmentFunction {
	void unassign(RBACSubject subject) throws IOException;
}