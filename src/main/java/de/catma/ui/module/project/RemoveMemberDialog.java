package de.catma.ui.module.project;

import java.util.Set;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;

import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACUnAssignmentFunction;
import de.catma.user.Member;

public class RemoveMemberDialog extends AbstractMemberDialog<Void> {

	private final Set<Member> members;
	
	private final RBACUnAssignmentFunction unassignment;	
	
	private Grid<Member> ls_members;
	
	public RemoveMemberDialog(RBACUnAssignmentFunction unassignment,
			Set<Member> members, SaveCancelListener<Void> saveCancelListener) {
		super("Remove Members","Remove the selected Members from the Project", saveCancelListener);
		this.members = members;
		this.unassignment = unassignment;
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		ls_members = new Grid<>("Members", members);
		ls_members.setWidth("100%");
		ls_members.addColumn(user -> user.preciseName()).setCaption("Name");
		ls_members.setHeight("100%");
		content.addComponent(ls_members);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout)content).setExpandRatio(ls_members, 1f);
		}
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
		setWidth("70%");
		setHeight("80%");

	}
}
