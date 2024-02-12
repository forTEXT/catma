package de.catma.ui.module.project;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.User;

public abstract class AbstractMemberDialog<T> extends AbstractOkCancelDialog<T> {

	protected ComboBox<User> cbUsers;
	protected ComboBox<RBACRole> cbRole; 
	protected Label descriptionLabel;
	protected DateField expiresAtInput;
		
	protected ErrorHandler errorLogger;
	
	public AbstractMemberDialog(String title, String description, 
			SaveCancelListener<T> saveCancelListener) {
		super(title, saveCancelListener);
		this.errorLogger = (ErrorHandler) UI.getCurrent();
		initComponents(description);
	}


	private void initComponents(String description) {
		
		this.descriptionLabel = new Label(description);
		descriptionLabel.setHeight("50px");
		
	    cbUsers = new ComboBox<>("Member");
		cbUsers.setWidth("100%");
		cbUsers.setPageLength(20);
		cbUsers.setItemCaptionGenerator(User::preciseName);
		
		cbRole = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.ASSISTANT, RBACRole.MAINTAINER));

		cbRole.setWidth("100%");
		cbRole.setItemCaptionGenerator(RBACRole::getRoleName);
		cbRole.setEmptySelectionAllowed(false);
		
		expiresAtInput = new DateField("Membership expires at (optional)");
		expiresAtInput.setDateFormat("yyyy/MM/dd");
		expiresAtInput.setPlaceholder("yyyy/mm/dd");
		expiresAtInput.setWidth("100%");

	}

}
