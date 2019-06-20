package de.catma.ui.modules.project;

import java.util.Set;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACUnAssignmentFunction;
import de.catma.user.Member;
import de.catma.user.User;

public class RemoveMemberDialog extends AbstractMemberDialog<Void> {

	private final Set<Member> members;
	
	private final RBACUnAssignmentFunction unassignment;	
	
	private ListSelect<Member> ls_members;
	
	public RemoveMemberDialog(RBACUnAssignmentFunction unassignment,
			Set<Member> members, SaveCancelListener<Void> saveCancelListener) {
		super("Remove member","Removes the selected memeber from current project", saveCancelListener);
		this.members = members;
		this.unassignment = unassignment;
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		ls_members = new ListSelect<>("Members", members);
		ls_members.setReadOnly(true);
		ls_members.setWidth("100%");
		ls_members.setItemCaptionGenerator(User::preciseName);
		content.addComponent(ls_members);		
	}
	
	@Override
	protected Void getResult() {
		try {
			for(Member member : members){
				unassignment.unassign(member);
			}
			return null;
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	protected void layoutWindow() {
		super.layoutWindow();
		setWidth("320px");
		setHeight("550px");

	}
}
