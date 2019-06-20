package de.catma.ui.rbac;

import java.io.IOException;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;

public interface RBACAssignmentFunction {
	RBACSubject assign(RBACSubject subject, RBACRole role) throws IOException ;
}