package de.catma.ui.modules.project;

import com.google.common.collect.Lists;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.user.User;

public abstract class AbstractMemberDialog<T> extends AbstractOkCancelDialog<T> {

	protected final ComboBox<User> cb_users = new ComboBox<>("member");
	protected final ComboBox<RBACRole> cb_role = new ComboBox<RBACRole>("role", 
			Lists.newArrayList(RBACRole.values())); 
	protected final Label l_description;
		
	protected ErrorHandler errorLogger;
	
	public AbstractMemberDialog(String title, String description, 
			SaveCancelListener<T> saveCancelListener) {
		super(title, saveCancelListener);
		this.l_description = new Label(description);
		l_description.setHeight("50px");
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
		
		cb_users.setWidth("100%");
		cb_users.setPageLength(20);
		cb_users.setItemCaptionGenerator(User::getIdentifier);

		cb_role.setWidth("100%");
		cb_role.setItemCaptionGenerator(RBACRole::getRolename);
		cb_role.setEmptySelectionAllowed(false);
	}


	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(l_description);
		content.addComponent(cb_users);
		content.addComponent(cb_role);
		content.setHeightUndefined();
	}

	@Override
	protected void layoutButtonPanel(ComponentContainer content) {
		super.layoutButtonPanel(content);
		((AbstractOrderedLayout)content).setExpandRatio(getButtonPanel(), 1f);
	}
}
