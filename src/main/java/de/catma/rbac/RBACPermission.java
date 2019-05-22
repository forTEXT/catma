package de.catma.rbac;

/**
 * Roles that are mapped 1:1 to predefined gitlab roles called <code>AccessLevel</code> 
 * @author db
 *
 */
public enum RBACPermission {
	PROJECT_DELETE(RBACRole.OWNER),
	PROJECT_EDIT(RBACRole.MASTER),
	PROJECT_MEMBERS_EDIT(RBACRole.MASTER),
	DOCUMENT_CREATE_OR_UPLOAD(RBACRole.MASTER),
	DOCUMENT_DELETE_OR_EDIT(RBACRole.MASTER),
	DOCUMENT_READ(RBACRole.REPORTER),
	COLLECTION_CREATE(RBACRole.MASTER),
	COLLECTION_DELETE_OR_EDIT(RBACRole.MASTER),
	COLLECTION_WRITE(RBACRole.DEVELOPER),
	COLLECTION_READ(RBACRole.REPORTER),
	TAGSET_CREATE_OR_UPLOAD(RBACRole.MASTER),
	TAGSET_DELETE_OR_EDIT(RBACRole.MASTER),
	TAGSET_WRITE(RBACRole.DEVELOPER),
	TAGSET_READ(RBACRole.REPORTER),
	;
	
	private final RBACRole roleRequired;
	
	private RBACPermission(RBACRole roleRequired) {
		this.roleRequired = roleRequired;
	}

	public RBACRole getRoleRequired() {
		return roleRequired;
	}
	
}
