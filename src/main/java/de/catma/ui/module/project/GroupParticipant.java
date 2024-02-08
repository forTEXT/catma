package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.rbac.RBACRole;
import de.catma.user.SharedGroup;

public class GroupParticipant implements ProjectParticipant {


	private final SharedGroup sharedGroup;

	public GroupParticipant(SharedGroup sharedGroup) {
		this.sharedGroup = sharedGroup;
	}

	@Override
	public Long getId() {
		return sharedGroup.groupId();
	}
	
	@Override
	public String getName() {
		return sharedGroup.name();
	}

	@Override
	public String getIcon() {
		return VaadinIcons.USERS.getHtml();
	}

	@Override
	public RBACRole getRole() {
		return sharedGroup.roleInProject();
	}

	@Override
	public boolean isGroup() {
		return true;
	}

	@Override
	public String getDescription() {
		return "User group " + getName();
	}

	public SharedGroup getSharedGroup() {
		return sharedGroup;
	}
	
	@Override
	public boolean isDirect() {
		return true;
	}
}
