package de.catma.ui.module.project;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.user.Member;
import de.catma.user.User;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EditMemberDialog extends AbstractMemberDialog<Set<RBACSubject>> {
	private final Set<Member> members;
	private final RBACAssignmentFunction rbacAssignmentFunction;

	private final RBACRole defaultRole;

	public EditMemberDialog(
			RBACAssignmentFunction rbacAssignmentFunction,
			Set<Member> members,
			SaveCancelListener<Set<RBACSubject>> saveCancelListener
	) {
		super("Update Members","Change the role of the members below", saveCancelListener);

		this.members = members;
		this.rbacAssignmentFunction = rbacAssignmentFunction;

		Optional<Member> memberWithLowestRole = members.stream().sorted(
				Comparator.comparingInt(member -> member.getRole().getAccessLevel())
		).findFirst();
		this.defaultRole = memberWithLowestRole.isPresent() ? memberWithLowestRole.get().getRole() : RBACRole.ASSISTANT;
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

		content.addComponent(cbRole);

		cbRole.setValue(defaultRole);
	}

	@Override
	protected Set<RBACSubject> getResult() {
		try {
			Set<RBACSubject> result = new HashSet<>();
			for (Member member : members) {
				result.add(rbacAssignmentFunction.assign(member, cbRole.getValue()));
			}
			return result;
		}
		catch (Exception e) {
			errorLogger.showAndLogError(null, e);
			return null;
		}
	}

	@Override
	protected void layoutWindow() {
		setWidth("70%");
		setHeight("80%");
	}
}
