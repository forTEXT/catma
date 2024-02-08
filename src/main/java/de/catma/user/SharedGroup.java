package de.catma.user;

import de.catma.rbac.RBACRole;

public record SharedGroup(Long groupId, String name, RBACRole roleInProject) {}
