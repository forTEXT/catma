package de.catma.user;

import de.catma.rbac.RBACRole;

public record SharedGroup(Long groupId, String name, RBACRole roleInProject) implements Comparable<SharedGroup>{

	@Override
	public int compareTo(SharedGroup o) {
		return this.groupId.compareTo(o.groupId);
	}
}
