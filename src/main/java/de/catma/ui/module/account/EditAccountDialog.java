package de.catma.ui.module.account;

import java.io.IOException;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.main.auth.ChangePasswordValidator;
import de.catma.user.User;
import de.catma.user.UserData;

public class EditAccountDialog extends Window {
	private final RemoteGitManagerPrivileged gitManagerPrivileged;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;

	private final long userId;
	private final String name;
	private final String username;

	private final Binder<UserData> userBinder = new Binder<>();
	private final UserData userData = new UserData();

	private PasswordField pfPassword;
	private PasswordField pfVerifyPassword;
	private Button btnSave;
	private Button btnCancel;

	public EditAccountDialog(RemoteGitManagerPrivileged gitManagerPrivileged, LoginService loginService, RemoteGitManagerRestricted remoteGitManagerRestricted) {
		this.gitManagerPrivileged = gitManagerPrivileged;
		this.remoteGitManagerRestricted = remoteGitManagerRestricted;

		User user = loginService.getRemoteGitManagerRestricted().getUser();
		this.userId = user.getUserId();
		this.name = user.getName();
		this.username = user.getIdentifier();

		this.setCaption("Account Details");
		initComponents();
		initActions();
	}

	private void initActions() {
		btnCancel.addClickListener(evt -> close());

		btnSave.addClickListener(evt -> {
			// sanity check the password
			if(!pfPassword.getValue().equals(pfVerifyPassword.getValue())) {
				Notification.show("The passwords don't match!", Type.ERROR_MESSAGE);
				return;
			}

			// validate the bean!
			try {
				userBinder.writeBean(userData);
			}
			catch (ValidationException e) {
				Notification.show(
						Joiner.on("\n").join(
								e.getValidationErrors().stream().map(ValidationResult::getErrorMessage).collect(Collectors.toList())
						),
						Type.ERROR_MESSAGE
				);
				return;
			}

			try {
				gitManagerPrivileged.modifyUserAttributes(
						userId,
						userData.getName(),
						userData.getPassword().isEmpty() ? null : userData.getPassword()
				);

				remoteGitManagerRestricted.refreshUser();
				
				Notification.show("Account details updated", Type.TRAY_NOTIFICATION);
			}
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to update account details", e);
			}

			this.close();
		});
	}

	private void initComponents(){
		setWidth("40%");
		setHeight("80%");
		setModal(true);
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		Label lDescription = new Label("Edit your account details", ContentMode.HTML);
		
		TextField tfName = new TextField("Public name");
		tfName.setWidth("100%");
		if(name != null){
			tfName.setValue(name);
		}
		TextField tfUsername = new TextField("Username");
		tfUsername.setWidth("100%");
		if(username != null){
			tfUsername.setValue(username);
		}
		tfUsername.setEnabled(false);
		
		pfPassword = new PasswordField("Password");
		pfPassword.setWidth("100%");
		
		pfVerifyPassword = new PasswordField("Verify password");
		pfVerifyPassword.setWidth("100%");
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		btnSave = new Button("Save");
		btnCancel = new Button("Cancel");
		
		buttonPanel.addComponent(btnSave);
		buttonPanel.addComponent(btnCancel);

		buttonPanel.setComponentAlignment(btnCancel, Alignment.BOTTOM_RIGHT);
		buttonPanel.setComponentAlignment(btnSave, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btnSave, 1f);
		
		content.addComponent(lDescription);
		content.addComponent(tfName);
		content.addComponent(tfUsername);
		content.addComponent(pfPassword);
		content.addComponent(pfVerifyPassword);
		content.setExpandRatio(pfVerifyPassword, 1f);
		
		content.addComponent(buttonPanel);
		
		userBinder.forField(tfName)
		.bind(UserData::getName, UserData::setName);
		userBinder.forField(pfPassword)
	    .withValidator(new ChangePasswordValidator(8))
	    .bind(UserData::getPassword, UserData::setPassword);
		
		setContent(content);
	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
