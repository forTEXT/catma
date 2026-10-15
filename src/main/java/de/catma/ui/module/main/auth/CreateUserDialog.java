package de.catma.ui.module.main.auth;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;

import de.catma.ui.Parameter;
import de.catma.ui.ParameterProvider;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.UserData;
import de.catma.user.signup.SignupTokenManager.TokenAction;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Dialog for user creation. The email address has already been verified and must not be changed.
 * <p>
 * The account is created on the GitLab backend, after which the user is sent through the OAuth flow to sign in with the credentials they just chose (we can't
 * sign them in directly, as that would require the deprecated and since removed resource owner password credentials grant).
 */
public class CreateUserDialog extends AuthenticationDialog {
	private final RemoteGitManagerPrivileged gitlabManagerPrivileged;

	private final Binder<UserData> userDataBinder = new Binder<>();
	private final UserData userData = new UserData();

	private final PasswordValidator passwordValidator;

	private TextField tfUsername;

	public CreateUserDialog(final String emailAddress) {
		super("Complete Sign-up");
		this.setClosable(false);

		this.gitlabManagerPrivileged = new GitlabManagerPrivileged();

		this.userData.setEmail(emailAddress);
		this.passwordValidator = new PasswordValidator(emailAddress);

		initComponents();
	}

	private void initComponents() {
		setWidth("50%");
		setHeight("80%");
		setModal(true);

		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setStyleName("create-user-dialog");

		Label lblDescription = new Label(
				"Please complete your sign-up by filling out this form, then click \"Continue\" to sign in via Gitlab and start using CATMA. (CATMA is "
						+ "built on GitLab, which we use to manage your account and data.)<br /><br />",
				ContentMode.HTML
		);
		lblDescription.setWidth("100%");

		TextField tfEmail = new TextField("Email Address");
		tfEmail.setWidth("100%");
		tfEmail.setValue(userData.getEmail());
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Email address already verified");

		tfUsername = new TextField("Username");
		tfUsername.setWidth("100%");

		PasswordField tfPassword = new PasswordField("Password");
		tfPassword.setWidth("100%");

		PasswordField tfVerifyPassword = new PasswordField("Verify password");
		tfVerifyPassword.setWidth("100%");

		Label lblPasswordRequirements = new Label(
				"Password Requirements:<br />"
						+ "<ul>"
						+ "<li>At least 8 characters (max. 128)</li>"
						+ "<li>Must not contain part of your email address or username</li>"
						+ "<li>Must not contain a predictable word (for example, \"catma\")</li>"
						+ "<li>Must not be a known weak password (for example, \"password\", \"Password1\", etc.)</li>"
						+ "</ul>",
				ContentMode.HTML
		);
		lblPasswordRequirements.setWidth("100%");
		lblPasswordRequirements.setStyleName("password-requirements");

		Button btnCreate = new Button("Continue");

		content.addComponent(lblDescription);
		content.addComponent(tfEmail);
		content.addComponent(tfUsername);
		content.addComponent(tfPassword);
		content.addComponent(tfVerifyPassword);
		content.addComponent(lblPasswordRequirements);
		content.setExpandRatio(lblPasswordRequirements, 1f);
		content.addComponent(btnCreate);
		content.setComponentAlignment(btnCreate, Alignment.BOTTOM_RIGHT);

		userDataBinder.forField(tfEmail)
				.bind(UserData::getEmail, null);

		userDataBinder.forField(tfUsername)
				.asRequired("Username is required")
				.withValidator(new UsernameValidator(gitlabManagerPrivileged))
				.bind(UserData::getUsername, UserData::setUsername);

		Binder.Binding<UserData, String> passwordBinding = userDataBinder.forField(tfPassword)
				.asRequired("Password is required")
				.withValidator(passwordValidator)
				.bind(UserData::getPassword, UserData::setPassword);

		Binder.Binding<UserData, String> verifyPasswordBinding = userDataBinder.forField(tfVerifyPassword)
				.asRequired("Verify password is required")
				.withValidator(verifyPassword -> verifyPassword.equals(tfPassword.getValue()), "The passwords must match")
				.bind(UserData::getPassword, (UserData userData, String verifyPassword) -> {}); // no-op setter


		tfUsername.addValueChangeListener(event -> {
			passwordValidator.setUsername(event.getValue());
			// only validate the password if has been entered already - prevents a validation error from being displayed unnecessarily
			if (tfPassword.getValue() != null && !tfPassword.getValue().isEmpty()) {
				passwordBinding.validate();
			}
		});
		tfPassword.addValueChangeListener(event -> verifyPasswordBinding.validate());

		btnCreate.addClickListener(click -> {
			// validate the bean
			try {
				userDataBinder.writeBean(userData);
			}
			catch (ValidationException e) {
				Notification.show(
						Joiner.on("\n").join(
								e.getValidationErrors().stream()
										.map(ValidationResult::getErrorMessage)
										.collect(Collectors.toList())
						),
						Type.ERROR_MESSAGE
				);
				return;
			}

			try {
				gitlabManagerPrivileged.createUser(
						userData.getEmail(),
						userData.getUsername(),
						userData.getPassword(),
						userData.getUsername()
				);

				// send the user through the OAuth flow to sign in with the credentials they just chose
				// an account signup token has been consumed by the time we get here, so forwarding it would just fail to validate on the way back - but a
				// group/project invitation token is deliberately kept alive so that the invitation can be accepted once the account exists (see
				// RequestTokenHandler), and dropping it would leave the user signed in but not a member
				TokenAction tokenAction = TokenAction.findAction(((ParameterProvider) UI.getCurrent()).getParameter(Parameter.ACTION));
				redirectToGitLabOauth(tokenAction != TokenAction.verify);
			}
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to create user", e);
			}
		});


		setContent(content);
	}

	@Override
	public void attach() {
		super.attach();
		tfUsername.focus();
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
