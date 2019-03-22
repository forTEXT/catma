package de.catma.ui.modules.project;

import com.google.common.collect.Lists;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.user.User;

public abstract class AbstractMemberDialog extends AbstractOkCancelDialog<RBACSubject> {

	protected final ComboBox<User> cb_users = new ComboBox<>("member");
	protected final ComboBox<RBACRole> cb_role = new ComboBox<RBACRole>("role", 
			Lists.newArrayList(RBACRole.values())); 
	protected final Label l_description;
	
	protected IRemoteGitManagerRestricted remoteGitManager;
	
	protected ErrorHandler errorLogger;
	
	public AbstractMemberDialog(String title, String description, IRemoteGitManagerRestricted remoteGitManager, 
			SaveCancelListener<RBACSubject> saveCancelListener) {
		super(title, saveCancelListener);
		this.l_description = new Label(description);
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
		this.remoteGitManager = remoteGitManager;
		
		cb_users.setWidth("100%");
		cb_users.setPageLength(20);
		cb_users.setItemCaptionGenerator(User::getIdentifier);

		cb_role.setWidth("100%");
		cb_role.setItemCaptionGenerator(RBACRole::name);
	}


	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(l_description);
		content.addComponent(cb_users);
		content.addComponent(cb_role);
		((AbstractOrderedLayout)content).setExpandRatio(l_description, 1f);
	}

}
