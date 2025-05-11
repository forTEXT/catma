package de.catma.ui.module.project;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.Pair;

public class EditMemberDialog extends AbstractMemberDialog<Pair<RBACRole, LocalDate>> {
	private final Set<ProjectParticipant> participants;

	private final RBACRole defaultRole;

	
	public EditMemberDialog(
			Set<ProjectParticipant> participants,
			SaveCancelListener<Pair<RBACRole, LocalDate>> saveCancelListener
	) {
		this(participants, Lists.newArrayList(RBACRole.ASSISTANT, RBACRole.MAINTAINER), saveCancelListener);
	}
	
	public EditMemberDialog(
			Set<ProjectParticipant> participants,
			List<RBACRole> availableRoles,
			SaveCancelListener<Pair<RBACRole, LocalDate>> saveCancelListener
	) {
		super("Update Members","Change the role of the members below", availableRoles, saveCancelListener);

		this.participants = participants;

		Optional<ProjectParticipant> memberWithLowestRole = participants.stream().sorted(
				Comparator.comparingInt(member -> member.getRole().getAccessLevel())
		).findFirst();
		this.defaultRole = memberWithLowestRole.isPresent() ? memberWithLowestRole.get().getRole() : RBACRole.ASSISTANT;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(descriptionLabel);

		ListSelect<ProjectParticipant> lsMembers = new ListSelect<>("Members", participants);
		lsMembers.setReadOnly(true);
		lsMembers.setSizeFull();
		lsMembers.setItemCaptionGenerator(ProjectParticipant::getDescription);
		content.addComponent(lsMembers);
		((AbstractOrderedLayout) content).setExpandRatio(lsMembers, 1f);

		content.addComponent(cbRole);
		content.addComponent(expiresAtInput);
		if (this.participants.size() == 1 && this.participants.iterator().next().getExpiresAt() != null) {
			expiresAtInput.setValue(this.participants.iterator().next().getExpiresAt());
		}
		cbRole.setValue(defaultRole);
	}

	@Override
	protected Pair<RBACRole, LocalDate> getResult() {
		return new Pair<>(cbRole.getValue(), expiresAtInput.getValue());
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
