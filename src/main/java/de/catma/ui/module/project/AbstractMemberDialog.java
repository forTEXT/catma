package de.catma.ui.module.project;

import com.google.common.collect.Lists;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.User;

public abstract class AbstractMemberDialog<T> extends AbstractOkCancelDialog<T> {

	protected ComboBox<User> cb_users;
	protected ComboBox<RBACRole> cb_role; 
	protected Label l_description;
		
	protected ErrorHandler errorLogger;
	
	public AbstractMemberDialog(String title, String description, 
			SaveCancelListener<T> saveCancelListener) {
		super(title, saveCancelListener);
		this.errorLogger = (ErrorHandler) UI.getCurrent();
		initComponents(description);
	}


	private void initComponents(String description) {
		
		this.l_description = new Label(description);
		l_description.setHeight("50px");
		
	    cb_users = new ComboBox<>("Member");
		cb_users.setWidth("100%");
		cb_users.setPageLength(20);
		cb_users.setItemCaptionGenerator(User::preciseName);
		
		cb_role = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.GUEST, RBACRole.REPORTER, RBACRole.ASSISTANT, RBACRole.MAINTAINER));

		cb_role.setWidth("100%");
		cb_role.setItemCaptionGenerator(RBACRole::getRolename);
		cb_role.setEmptySelectionAllowed(false);
	}


	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(l_description);
		content.addComponent(cb_users);
		content.addComponent(cb_role);
	}

	@Override
	protected void layoutButtonPanel(ComponentContainer content) {
		super.layoutButtonPanel(content);
		((AbstractOrderedLayout)content).setExpandRatio(getButtonPanel(), 1f);
	}
}
