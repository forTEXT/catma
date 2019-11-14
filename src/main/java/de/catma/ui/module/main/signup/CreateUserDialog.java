package de.catma.ui.module.main.signup;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.server.Page;
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

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Dialog for User creation. Email has already been verified and must not be changed.
 * 
 * @author db
 *
 */
@JavaScript({"https://www.google.com/recaptcha/api.js?render=6LenwosUAAAAAKfYcN4ZAGMu1QwfECD2cZPjLoFG"})
public class CreateUserDialog extends Window {

	private UserData user = new UserData();
	private GoogleVerificationResult recaptchaResult = new GoogleVerificationResult();

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private final Binder<UserData> userBinder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
	private final SignupToken signupToken;
	
	public CreateUserDialog(String caption, SignupToken signupToken) {
		super(caption);
		this.signupToken = signupToken;
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
		tfEmail.setValue(signupToken.getEmail());
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Email is already been verified");
		
		PasswordField tfPassword = new PasswordField("Password");
		tfPassword.setWidth("100%");
		
		PasswordField tfVerifyPassword = new PasswordField("Verify password");
		tfVerifyPassword.setWidth("100%");
		
		TextField hiddenVerification = new TextField();
		hiddenVerification.addStyleName("g-recaptcha-response");
		

		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		Button btnCreate = new Button("Create");
		btnCreate.setEnabled(false);
		btnCreate.setDescription("Awaiting Google recaptcha verification...");
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

		hiddenVerification.addValueChangeListener(event -> 		{
			recaptchaResult = new GoogleRecaptchaVerifier().verify(hiddenVerification.getValue());
			if (recaptchaResult.isSuccess()) {
				btnCreate.setEnabled(true);
				btnCreate.setDescription("Create user account");
			}
			else {
				Notification.show(
					"Error", 
					"Recaptcha verfication failed: " 
							+ Arrays.asList(recaptchaResult.getErrorCodes()), 
					Type.ERROR_MESSAGE);
			}			
			logger.info(recaptchaResult.toString());
		});
		
		content.addComponent(lDescription);
		content.addComponent(tfUsername);
		content.addComponent(tfEmail);
		content.addComponent(tfPassword);
		content.addComponent(tfVerifyPassword);
		content.setExpandRatio(tfVerifyPassword, 1f);
		
		content.addComponent(hiddenVerification);
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
			// Check google recaptcha
			if(hiddenVerification.getValue() == null || hiddenVerification.getValue().isEmpty()){
				Notification.show("reCaptcha verification failed!",Type.ERROR_MESSAGE);
				return;
			}
						
			if(! recaptchaResult.isSuccess() && recaptchaResult.getScore() < 0.5f){
				Notification.show("reCaptcha verification failed!",Type.ERROR_MESSAGE);
				logger.warning("recaptcha failed: " + recaptchaResult);
				return;
			}

			// sanity check the password
			if(tfPassword.getValue() == null || tfVerifyPassword.getValue() == null || 
					(! tfPassword.getValue().equals(tfVerifyPassword.getValue()))) {
				Notification.show("Passwords don't match",Type.ERROR_MESSAGE);
				return;
			}

			// validate the bean!
			try {
				userBinder.writeBean(user);
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
						user.getEmail(), 
						user.getUsername(), user.getPassword(), 
						user.getUsername());
				
				Notification.show(
						"Your user account has been created. Please sign in!", 
						Type.HUMANIZED_MESSAGE);
				
			} catch (IOException e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't create token in backend", e);
			}
			this.close();
		});
		setContent(content);
	}

	/**
	 * Displays this dialog and registers the javascript callback for google recaptcha
	 */
	public void show() {
		UI.getCurrent().addWindow(this);
		com.vaadin.ui.JavaScript.getCurrent().execute(
		  "grecaptcha.ready(function() {"+
		  " grecaptcha.execute('6LenwosUAAAAAKfYcN4ZAGMu1QwfECD2cZPjLoFG', {action: 'CreateUserCatma'})"+
		  " .then(function(token) {"+
		  "  document.getElementsByClassName('g-recaptcha-response')[0].value = token;"+
		  "  document.getElementsByClassName('g-recaptcha-response')[0].dispatchEvent(new Event('change'));"+
		  " }); "+
		  "}); ");
		  
	}
	
	@Override
	public void close() {
		super.close();
		Page.getCurrent().replaceState(CATMAPropertyKey.BaseURL.getValue());
	}
}
