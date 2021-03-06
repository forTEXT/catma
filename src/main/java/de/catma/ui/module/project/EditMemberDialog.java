package de.catma.ui.module.project;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.user.Member;
import de.catma.user.User;

public class EditMemberDialog extends AbstractMemberDialog<Set<RBACSubject>> {

	private final Set<Member> members;
	
	private final RBACAssignmentFunction assignment;

	private final Binder<RBACRole> roleBinder = new Binder<>();
	
	
	private RBACRole accesslevel;
	private ListSelect<Member> ls_members;
	
	public EditMemberDialog(
			RBACAssignmentFunction assignment,
			Set<Member> members, SaveCancelListener<Set<RBACSubject>> saveCancelListener) {
		super("Update Members","Change the role of the selected Members", saveCancelListener);
		this.members = members;
		this.assignment = assignment;
		this.accesslevel = members.isEmpty() ?  RBACRole.REPORTER : members.iterator().next().getRole();
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		ls_members = new ListSelect<>("Members", members);
		ls_members.setReadOnly(true);
		ls_members.setSizeFull();
		ls_members.setItemCaptionGenerator(User::preciseName);
		
		content.addComponent(descriptionLabel);
		content.addComponent(ls_members);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout) content).setExpandRatio(ls_members, 1f);
		}
		content.addComponent(cbRole);
		
		roleBinder
			.forField(cbRole)
			.withValidator((role,context) ->  role.equals(RBACRole.OWNER)?  ValidationResult.error("Changing the owner is not allowed") : ValidationResult.ok());
		cbRole.setValue(accesslevel);
	}
	
	@Override
	protected void handleOkPressed() {
		BinderValidationStatus<RBACRole> validationResult = roleBinder.validate();
		
		if(validationResult.isOk()){
		super.handleOkPressed();
		} else {
			Notification.show("Role not allowed", Joiner.on(',').join(validationResult.getValidationErrors()) , Notification.Type.WARNING_MESSAGE);
		}
	};
	
	@Override
	protected Set<RBACSubject> getResult() {
		try {
			Set<RBACSubject> result = new HashSet<>();
			for(Member member : members){
				result.add( assignment.assign(member, cbRole.getValue()));
			}
			return result;
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
