package de.catma.ui.rbac;

import java.io.IOException;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;

//	private final String projectId;
public interface RBACAssignmentFunction<D> {
	RBACSubject assign(RBACSubject subject, RBACRole role, D resourceOrProject) throws IOException ;
}