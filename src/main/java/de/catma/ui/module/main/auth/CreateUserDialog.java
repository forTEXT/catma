package de.catma.ui.module.main.auth;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;

import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.sqlite.SqliteService;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.UserData;
import de.catma.user.signup.AccountSignupToken;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Dialog for user creation. The email address has already been verified and must not be changed.
 */
public class CreateUserDialog extends Window {
	private final String emailAddress;
	private final RemoteGitManagerPrivileged gitlabManagerPrivileged;

	private final Binder<UserData> userBinder = new Binder<>();
	private final UserData userData = new UserData();
	
	private final EventBus eventBus;
	private final InitializationService initService;
	private final LoginService loginservice;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;


	public CreateUserDialog(
			String caption, final String emailAddress, EventBus eventBus, 
			LoginService loginservice, InitializationService initService, HazelCastService hazelCastService, SqliteService sqliteService) {
		super(caption);

		this.emailAddress = emailAddress;
		this.gitlabManagerPrivileged = new GitlabManagerPrivileged();
		this.eventBus = eventBus;
		this.loginservice = loginservice;
		this.initService = initService;
		this.hazelCastService = hazelCastService;
		this.sqliteService = sqliteService;

		setWidth("50%");
		setHeight("80%");

		initComponents();
	}

	private void initComponents() {
		setModal(true);
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		Label lDescription = new Label("Please complete your sign-up by filling out this form:", ContentMode.HTML);
		
		TextField tfUsername = new TextField("Username");
		tfUsername.setWidth("100%");
		
		TextField tfEmail = new TextField("Email");
		tfEmail.setWidth("100%");
		tfEmail.setValue(emailAddress);
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Email is already been verified");
		
		PasswordField tfPassword = new PasswordField("Password");
		tfPassword.setWidth("100%");
		
		PasswordField tfVerifyPassword = new PasswordField("Verify password");
		tfVerifyPassword.setWidth("100%");

		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		Button btnCreate = new Button("Create");
		Button btnCancel = new Button("Cancel");
		
		buttonPanel.addComponent(btnCreate);
		buttonPanel.addComponent(btnCancel);

		buttonPanel.setComponentAlignment(btnCancel, Alignment.BOTTOM_RIGHT);
		buttonPanel.setComponentAlignment(btnCreate, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btnCreate, 1f);
		
		btnCancel.addClickListener(evt -> { 
			Notification.show("User creation aborted", Type.TRAY_NOTIFICATION);
			this.close();
		});

		content.addComponent(lDescription);
		content.addComponent(tfUsername);
		content.addComponent(tfEmail);
		content.addComponent(tfPassword);
		content.addComponent(tfVerifyPassword);
		content.setExpandRatio(tfVerifyPassword, 1f);
		
		content.addComponent(buttonPanel);
		
		userBinder.forField(tfUsername) 
	    .withValidator(new UsernameValidator(gitlabManagerPrivileged))
	    .bind(UserData::getUsername, UserData::setUsername);
		
		userBinder.forField(tfEmail)
	    .bind(UserData::getEmail, UserData::setEmail);
		
		userBinder.forField(tfPassword)
	    .withValidator(new PasswordValidator(8))
	    .bind(UserData::getPassword, UserData::setPassword);

		btnCreate.addClickListener(click -> {
			// sanity check the password
			if(tfPassword.getValue() == null || tfVerifyPassword.getValue() == null || 
					(! tfPassword.getValue().equals(tfVerifyPassword.getValue()))) {
				Notification.show("The passwords don't match!",Type.ERROR_MESSAGE);
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
				gitlabManagerPrivileged.createUser(
						userData.getEmail(),
						userData.getUsername(), userData.getPassword(),
						userData.getUsername());
				
				loginservice.login(userData.getUsername(), userData.getPassword());
				
				initMainView();
				
			} catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Couldn't create token in backend", e);
			}
			this.close();
		});
		setContent(content);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}

	@Override
	public void close() {
		super.close();
		Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
	}
	
	private void initMainView() {
		Component mainView = initService.newEntryPage(eventBus, loginservice, hazelCastService, sqliteService);
		UI.getCurrent().setContent(mainView);
		eventBus.post(new RouteToDashboardEvent());
		close();
	}
	
}
