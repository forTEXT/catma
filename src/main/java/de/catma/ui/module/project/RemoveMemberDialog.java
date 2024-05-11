package de.catma.ui.module.project;

import java.util.Set;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.ui.dialog.SaveCancelListener;

public class RemoveMemberDialog extends AbstractMemberDialog<Set<ProjectParticipant>> {
	private final Set<ProjectParticipant> members;

	public RemoveMemberDialog(
			String targetName,
			Set<ProjectParticipant> members,
			SaveCancelListener<Set<ProjectParticipant>> saveCancelListener
	) {
		super(
				"Remove Members",
				String.format("Confirm that you would like to remove the members below from the %s", targetName),
				saveCancelListener
		);

		this.members = members;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(descriptionLabel);

		ListSelect<ProjectParticipant> lsMembers = new ListSelect<>("Members", members);
		lsMembers.setReadOnly(true);
		lsMembers.setSizeFull();
		lsMembers.setItemCaptionGenerator(ProjectParticipant::getDescription);
		content.addComponent(lsMembers);
		((AbstractOrderedLayout) content).setExpandRatio(lsMembers, 1f);
	}

	@Override
	protected Set<ProjectParticipant> getResult() {
		return members;
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
