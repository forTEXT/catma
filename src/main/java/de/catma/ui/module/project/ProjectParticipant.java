package de.catma.ui.module.project;

import java.time.LocalDate;

import de.catma.rbac.RBACRole;

public interface ProjectParticipant {

	Long getId();
	String getIcon();
	RBACRole getRole();
	boolean isGroup();
	String getName();
	String getDescription();
	boolean isDirect();
	
	LocalDate getExpiresAt();
	
}
