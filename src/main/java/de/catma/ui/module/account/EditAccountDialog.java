package de.catma.ui.module.account;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
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

import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.events.ChangeUserAttributeEvent;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.main.signup.ChangePasswordValidator;
import de.catma.ui.module.main.signup.UserData;
import de.catma.user.User;

public class EditAccountDialog extends Window {

	private UserData userData = new UserData();
	
	private final Binder<UserData> userBinder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitManagerPrivileged ;
	private final EventBus eventBus;

	private final String email;
	private final String username;
	private final String name;
	private final int userId;

	private Button btnCancel;

	private Button btnSave;

	private PasswordField tfPassword;

	private PasswordField tfVerifyPassword;
	
	public EditAccountDialog(IRemoteGitManagerPrivileged gitManagerPrivileged, 
			LoginService loginService, EventBus eventBus) {
		this.gitManagerPrivileged = gitManagerPrivileged;
		this.eventBus = eventBus;
		
		User user = Objects.requireNonNull(loginService.getAPI()).getUser();
		this.email = user.getEmail();
		this.username = user.getIdentifier();
		this.name = user.getName();
		this.userId = user.getUserId();
	
		this.setCaption("Account details");
		initComponents();
		initActions();
	}



	private void initActions() {
		btnCancel.addClickListener(evt -> close());
		
		btnSave.addClickListener(click -> {

			// sanity check the password
			if(! tfPassword.getValue().equals(tfVerifyPassword.getValue())) {
				Notification.show("Passwords don't match",Type.ERROR_MESSAGE);
				return;
			}

			// validate the bean!
			try {
				userBinder.writeBean(userData);
			} catch (ValidationException e) {
				Notification.show(
						Joiner
						.on("\n")
						.join(
								e.getValidationErrors().stream()
								.map(msg -> msg.getErrorMessage())
								.collect(Collectors.toList())),Type.ERROR_MESSAGE);
				return;
			}
			try {
				gitManagerPrivileged.modifyUserAttributes(userId,
						userData.getName(), userData.getPassword().isEmpty()? null : userData.getPassword());
				
				eventBus.post(new ChangeUserAttributeEvent());
				Notification.show("Profile modification successful", Type.TRAY_NOTIFICATION);
				
			} catch (IOException e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't change profile", e);
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
		TextField tfEmail = new TextField("Email");
		tfEmail.setWidth("100%");
		tfEmail.setValue(email);
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Email has already been verified");
		
		tfPassword = new PasswordField("Password");
		tfPassword.setWidth("100%");
		
		tfVerifyPassword = new PasswordField("Verify password");
		tfVerifyPassword.setWidth("100%");
		
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
		content.addComponent(tfEmail);
		content.addComponent(tfPassword);
		content.addComponent(tfVerifyPassword);
		content.setExpandRatio(tfVerifyPassword, 1f);
		
		content.addComponent(buttonPanel);
		
		userBinder.forField(tfName)
		.bind(UserData::getName, UserData::setName);
		userBinder.forField(tfPassword)
	    .withValidator(new ChangePasswordValidator(8))
	    .bind(UserData::getPassword, UserData::setPassword);
		
		setContent(content);

	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}
	
}
