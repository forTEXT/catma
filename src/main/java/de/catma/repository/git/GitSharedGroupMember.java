package de.catma.repository.git;

import java.util.Date;

import de.catma.rbac.RBACRole;
import de.catma.user.SharedGroup;
import de.catma.user.SharedGroupMember;

public class GitSharedGroupMember extends GitMember implements SharedGroupMember {

	private final SharedGroup sharedGroup;
	
	public GitSharedGroupMember(org.gitlab4j.api.models.Member member, SharedGroup sharedGroup) {
		super(member);
		this.sharedGroup = sharedGroup;
	}
	
	@Override
	public SharedGroup getSharedGroup() {
		return sharedGroup;
	}
	
	@Override
	public RBACRole getRole() {
		return sharedGroup.roleInProject();
	}

}
