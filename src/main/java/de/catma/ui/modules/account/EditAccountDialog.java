package de.catma.ui.modules.account;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.events.ChangeUserAttributeEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.modules.main.signup.ChangePasswordValidator;
import de.catma.ui.modules.main.signup.UserData;
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
	
	@Inject
	public EditAccountDialog(IRemoteGitManagerPrivileged gitManagerPrivileged, 
			LoginService loginService, EventBus eventBus) {
		this.gitManagerPrivileged = gitManagerPrivileged;
		this.eventBus = eventBus;
		
		User user = Objects.requireNonNull(loginService.getAPI()).getGitUser();
		this.email = user.getEmail();
		this.username = user.getIdentifier();
		this.name = user.getName();
		this.userId = user.getUserId();
	
		this.setCaption("Account details");
		initComponents();
	}



	private void initComponents(){
		setWidth("400px");
		setModal(true);
		
		VerticalLayout content = new VerticalLayout();
		content.addStyleName("spacing");
		content.addStyleName("margin");
		Label lDescription = new Label("description", ContentMode.HTML);
		lDescription.setValue("Edit your account details");
		
		TextField tfName = new TextField("Full name");
		tfName.setSizeFull();
		if(name != null){
			tfName.setValue(name);
		}
		TextField tfUsername = new TextField("Username");
		tfUsername.setSizeFull();
		if(username != null){
			tfUsername.setValue(username);
		}
		tfUsername.setEnabled(false);
		TextField tfEmail = new TextField("E-Mail");
		tfEmail.setSizeFull();
		tfEmail.setValue(email);
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Email is already verified");
		
		PasswordField tfPassword = new PasswordField("Password");
		tfPassword.setSizeFull();
		
		PasswordField tfVerifyPassword = new PasswordField("Verify Password");
		tfVerifyPassword.setSizeFull();
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setJustifyContent(JustifyContent.SPACE_BETWEEN);
		Button btnSave = new Button("Save");
		btnSave.setDescription("awaiting google verification");
		Button btnCancel = new Button("Cancel");
		
		buttonPanel.addComponent(btnCancel);
		buttonPanel.addComponent(btnSave);
		
		btnCancel.addClickListener(evt -> { 
			Notification.show("User creation aborted", Type.TRAY_NOTIFICATION);
			this.close();
		});
		
		content.addComponent(lDescription);
		content.addComponent(tfName);
		content.addComponent(tfUsername);
		content.addComponent(tfEmail);
		content.addComponent(tfPassword);
		content.addComponent(tfVerifyPassword);
		content.addComponent(buttonPanel);
		
		userBinder.forField(tfName)
		.bind(UserData::getName, UserData::setName);
		userBinder.forField(tfPassword)
	    .withValidator(new ChangePasswordValidator(8))
	    .bind(UserData::getPassword, UserData::setPassword);
		
		btnSave.addClickListener(click -> {

			// sanity check the password
			if(! tfPassword.getValue().equals(tfVerifyPassword.getValue())) {
				Notification.show("Passwords doen't match",Type.ERROR_MESSAGE);
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
		setContent(content);

	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}
	
}
