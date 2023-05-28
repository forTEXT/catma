package de.catma.ui.module.project;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACUnAssignmentFunction;
import de.catma.user.Member;
import de.catma.user.User;

import java.util.Set;

public class RemoveMemberDialog extends AbstractMemberDialog<Void> {
	private final Set<Member> members;
	private final RBACUnAssignmentFunction rbacUnAssignmentFunction;

	public RemoveMemberDialog(
			RBACUnAssignmentFunction rbacUnAssignmentFunction,
			Set<Member> members,
			SaveCancelListener<Void> saveCancelListener
	) {
		super(
				"Remove Members",
				"Confirm that you would like to remove the members below from the project",
				saveCancelListener
		);

		this.members = members;
		this.rbacUnAssignmentFunction = rbacUnAssignmentFunction;
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
	protected Void getResult() {
		try {
			for (Member member : members) {
				rbacUnAssignmentFunction.unassign(member);
			}
		}
		catch (Exception e) {
			errorLogger.showAndLogError(null, e);
		}
		return null;
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
