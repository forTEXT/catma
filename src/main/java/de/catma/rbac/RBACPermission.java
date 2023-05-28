package de.catma.rbac;

/**
 * Roles that are mapped 1:1 to predefined gitlab roles called <code>AccessLevel</code> 
 * @author db
 *
 */
public enum RBACPermission {
	PROJECT_DELETE(RBACRole.OWNER),
	PROJECT_EDIT(RBACRole.MAINTAINER),
	PROJECT_MEMBERS_EDIT(RBACRole.MAINTAINER),
	PROJECT_LEAVE(RBACRole.GUEST),	
	DOCUMENT_DELETE_OR_EDIT(RBACRole.MAINTAINER),
	COLLECTION_DELETE_OR_EDIT(RBACRole.MAINTAINER),
	TAGSET_DELETE_OR_EDIT(RBACRole.MAINTAINER),
	;
	
	private final RBACRole roleRequired;
	
	private RBACPermission(RBACRole roleRequired) {
		this.roleRequired = roleRequired;
	}

	public RBACRole getRoleRequired() {
		return roleRequired;
	}
	
}
