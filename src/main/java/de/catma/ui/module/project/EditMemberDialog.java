package de.catma.ui.module.project;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.SaveCancelListener;

public class EditMemberDialog extends AbstractMemberDialog<RBACRole> {
	private final Set<ProjectParticipant> participants;

	private final RBACRole defaultRole;

	public EditMemberDialog(
			Set<ProjectParticipant> participants,
			SaveCancelListener<RBACRole> saveCancelListener
	) {
		super("Update Members","Change the role of the members below", saveCancelListener);

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

		cbRole.setValue(defaultRole);
	}

	@Override
	protected RBACRole getResult() {
		return cbRole.getValue();
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
