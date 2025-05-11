package de.catma.ui.module.project;

import java.time.LocalDate;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontIcon;
import com.vaadin.server.Resource;

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
		return ((FontIcon)getIconAsResource()).getHtml();
	}
	
	@Override
	public Resource getIconAsResource() {
		return VaadinIcons.USERS;
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
	public LocalDate getExpiresAt() {
		return null;
	}
	
	@Override
	public boolean isDirect() {
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
