package de.catma.ui.module.dashboard;

import java.time.LocalDate;
import java.time.ZoneId;

import com.vaadin.icons.VaadinIcons;

import de.catma.rbac.RBACRole;
import de.catma.ui.module.project.ProjectParticipant;
import de.catma.user.Member;

public class GroupMemberParticipant implements ProjectParticipant {

	private final Member member;
	
	public GroupMemberParticipant(Member member) {
		super();
		this.member = member;
	}

	@Override
	public Long getId() {
		return member.getUserId();
	}

	@Override
	public String getIcon() {
		return VaadinIcons.USER.getHtml();
	}

	@Override
	public RBACRole getRole() {
		return member.getRole();
	}

	@Override
	public boolean isGroup() {
		return false;
	}

	@Override
	public String getName() {
		return member.getName();
	}

	@Override
	public String getDescription() {
		return member.preciseName();
	}

	@Override
	public boolean isDirect() {
		return true;
	}

	public Member getMember() {
		return member;
	}
	
	@Override
	public LocalDate getExpiresAt() {
		return member.getExpiresAt();
	}
}
