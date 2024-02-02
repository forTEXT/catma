package de.catma.ui.module.project;

import java.util.Set;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Member;
import de.catma.user.User;

public class RemoveMemberDialog extends AbstractMemberDialog<Set<Member>> {
	private final Set<Member> members;

	public RemoveMemberDialog(
			Set<Member> members,
			SaveCancelListener<Set<Member>> saveCancelListener
	) {
		super(
				"Remove Members",
				"Confirm that you would like to remove the members below from the project",
				saveCancelListener
		);

		this.members = members;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(descriptionLabel);

		ListSelect<Member> lsMembers = new ListSelect<>("Members", members);
		lsMembers.setReadOnly(true);
		lsMembers.setSizeFull();
		lsMembers.setItemCaptionGenerator(User::preciseName);
		content.addComponent(lsMembers);
		((AbstractOrderedLayout) content).setExpandRatio(lsMembers, 1f);
	}

	@Override
	protected Set<Member> getResult() {
		return members;
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
