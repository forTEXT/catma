package de.catma.ui.module.main.auth;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.mail.EmailException;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.UserData;
import de.catma.user.signup.SignupTokenManager;

/**
 * SignUpDialog allows users to sign up either by entering an email address (the password is set later), or via Google (OpenID Connect).
 * The email address option is protected against bots using Google reCAPTCHA.
 */
public class SignUpDialog extends AuthenticationDialog {
	private final Logger logger = Logger.getLogger(SignUpDialog.class.getName());

	private final RemoteGitManagerPrivileged gitManagerPrivileged;

	private final Binder<UserData> userDataBinder = new Binder<>();
	private final UserData userData = new UserData();

	private final String recaptchaVerificationStyleName = "g-recaptcha-response";
	private final String recaptchaVerificationAction = "CatmaSignUpForm";
	private final String recaptchaSiteKey = CATMAPropertyKey.GOOGLE_RECAPTCHA_SITE_KEY.getValue();

	private GoogleVerificationResult recaptchaResult;

	private TextField tfEmail;
	private Button btnSignup;
	private TextField hiddenVerification;

	private Button googleSignUpLink;

	public SignUpDialog(String caption) {
		super(caption);

		this.gitManagerPrivileged = new GitlabManagerPrivileged();
		this.recaptchaResult = new GoogleVerificationResult();

		initComponents();
		initActions();
	}

	private boolean isRecaptchaVerified() {
		return recaptchaSiteKey.equals("XXXXXXXXXXXX") || (recaptchaResult.isSuccess() && recaptchaResult.getScore() >= 0.5f && recaptchaResult.getAction().equals(recaptchaVerificationAction));
	}

	private void generateSignupTokenAndSendVerificationEmail() throws EmailException {
		SignupTokenManager signupTokenManager = new SignupTokenManager();
		signupTokenManager.sendAccountSignupVerificationEmail(userData);
	}

	private void initActions() {
		hiddenVerification.addValueChangeListener(event -> {
			// see loadAndExecuteRecaptcha
			recaptchaResult = new GoogleRecaptchaVerifier().verify(hiddenVerification.getValue());
			logger.info(recaptchaResult.toString());

			if (!recaptchaResult.isSuccess()) {
				logger.warning("Failed reCAPTCHA verification request, see previous log entries");
			}

			if (!isRecaptchaVerified()) {
				Notification.show(
						"Error",
						"reCAPTCHA verification failed\n" +
								"If you want to use sign up option 1, please reload the page to try again.",
						Notification.Type.ERROR_MESSAGE
				);
				btnSignup.setDescription("reCAPTCHA verification failed - reload to try again");
				return;
			}

			btnSignup.setEnabled(true);
			btnSignup.setDescription("");
		});

		btnSignup.addClickListener(click -> {
			// double-check the reCAPTCHA result just in case (no logging or user feedback as, under normal circumstances,
			// that would've already happened in the value change listener for the hidden verification field)
			if (!isRecaptchaVerified()) {
				return;
			}

			// validate the bean
			try {
				userDataBinder.writeBean(userData);
			}
			catch (ValidationException e) {
				Notification.show(
						Joiner
						.on("\n")
						.join(e.getValidationErrors()
								.stream()
								.map(ValidationResult::getErrorMessage)
								.collect(Collectors.toList())
						)
						, Notification.Type.ERROR_MESSAGE
				);
				return;
			}

			try {
				generateSignupTokenAndSendVerificationEmail();
				String completeMessage = "To complete your sign up, please click the link in the verification email!";

				if (CATMAPropertyKey.MAIL_SMTP_USER.getValue().equals("XXXXXXXXXXXX") &&
				    CATMAPropertyKey.MAIL_SMTP_PASS.getValue().equals("XXXXXXXXXXXX")) {
					completeMessage = "Email delivery isn't configured on this instance. See the logs to retrieve the user sign-up link.";
				}

				Notification completeSignupNotification = new Notification(
					completeMessage,
					Notification.Type.WARNING_MESSAGE
				);
				completeSignupNotification.setDelayMsec(-1);
				completeSignupNotification.show(Page.getCurrent());
			}
			catch (Exception e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError("Couldn't send verification email", e);
			}

			this.close();
		});

		googleSignUpLink.addClickListener(this::googleLinkClickListener);
	}

	private void initComponents() {
		setWidth("60%");
		setModal(true);

		VerticalLayout content = new VerticalLayout();
		content.setWidthFull();
		content.setStyleName("signup-dialog");

		Label lblChoice = new Label("Please choose one of the options below:");
		lblChoice.setWidth("100%");

		Panel pnlEmail = new Panel("Option 1: Email Address and Password");
		pnlEmail.setStyleName("email-panel");
		VerticalLayout pnlEmailContent = new VerticalLayout();

		tfEmail = new TextField("Email Address");
		tfEmail.setWidth("100%");

		userDataBinder.forField(tfEmail)
				.asRequired("Email address is required")
				.withValidator(new EmailValidator("Email address is invalid"))
				.withValidator(new AccountAlreadyTakenValidator(gitManagerPrivileged))
				.bind(UserData::getEmail, UserData::setEmail);

		HorizontalLayout hlEmailDescriptionAndButton = new HorizontalLayout();
		hlEmailDescriptionAndButton.setWidth("100%");

		Label lblDescription = new Label("description placeholder", ContentMode.HTML);
		lblDescription.setWidth("100%");
		lblDescription.setStyleName("description");
		lblDescription.setValue(
				"We'll send a verification email to the above address. Once your email address has been verified, you'll be able to " +
				"complete your profile by choosing a username and a password."
		);

		btnSignup = new Button("Sign Up");
		if (recaptchaSiteKey.equals("XXXXXXXXXXXX")) {
			btnSignup.setEnabled(true);
		} else {
			btnSignup.setEnabled(false);
			btnSignup.setDescription("Please wait a moment while we verify that you're not a bot...");
		}
		btnSignup.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		hlEmailDescriptionAndButton.addComponent(lblDescription);
		hlEmailDescriptionAndButton.addComponent(btnSignup);
		hlEmailDescriptionAndButton.setComponentAlignment(btnSignup, Alignment.BOTTOM_RIGHT);
		hlEmailDescriptionAndButton.setExpandRatio(lblDescription, 1f);

		hiddenVerification = new TextField();
		hiddenVerification.addStyleName(recaptchaVerificationStyleName);

		pnlEmailContent.addComponent(tfEmail);
		pnlEmailContent.addComponent(hlEmailDescriptionAndButton);
		pnlEmailContent.addComponent(hiddenVerification);
		pnlEmail.setContent(pnlEmailContent);

		Panel pnlGoogle = new Panel("Option 2: Google Account");
		pnlGoogle.setStyleName("google-panel");
		VerticalLayout pnlGoogleContent = new VerticalLayout();

		googleSignUpLink = new Button();
		googleSignUpLink.setIcon(new ThemeResource("img/google_buttons/btn_google_light_normal_sign_up.svg"));
		googleSignUpLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleSignUpLink.addStyleName("authdialog-google-login-link");

		pnlGoogleContent.addComponent(googleSignUpLink);
		pnlGoogle.setContent(pnlGoogleContent);

		HorizontalLayout hlLinks = new HorizontalLayout();
		hlLinks.setWidth("100%");
		hlLinks.setStyleName("links");

		Link termsOfUseLink = new Link(
				"Terms of Use",
				new ExternalResource(CATMAPropertyKey.TERMS_OF_USE_URL.getValue())
		);
		termsOfUseLink.setTargetName("_blank");
		termsOfUseLink.setStyleName("authdialog-tou-link");

		Label lblPipe = new Label("|");

		Link privacyPolicyLink = new Link(
				"Privacy Policy",
				new ExternalResource(CATMAPropertyKey.PRIVACY_POLICY_URL.getValue())
		);
		privacyPolicyLink.setTargetName("_blank");
		privacyPolicyLink.setStyleName("authdialog-pp-link");

		hlLinks.addComponent(termsOfUseLink);
		hlLinks.addComponent(lblPipe);
		hlLinks.addComponent(privacyPolicyLink);

		hlLinks.setComponentAlignment(termsOfUseLink, Alignment.BOTTOM_LEFT);
		hlLinks.setComponentAlignment(lblPipe, Alignment.BOTTOM_LEFT);
		hlLinks.setComponentAlignment(privacyPolicyLink, Alignment.BOTTOM_LEFT);
		hlLinks.setExpandRatio(privacyPolicyLink, 1f);

		content.addComponent(lblChoice);
		content.addComponent(pnlEmail);
		content.addComponent(pnlGoogle);
		content.addComponent(hlLinks);

		setContent(content);
	}

	@Override
	public void attach() {
		super.attach();
		tfEmail.focus();
	}

	private void loadAndExecuteRecaptcha() {
		// https://developers.google.com/recaptcha/docs/v3#programmatically_invoke_the_challenge
		// we originally used a @JavaScript annotation on the class to load the script, but this only allows constant values
		// (can't specify the site key dynamically)
		// https://stackoverflow.com/questions/14521108/dynamically-load-js-inside-js
		// note that exceptions are silently swallowed if not logged explicitly, hence the try ... catch
                if (!recaptchaSiteKey.equals("XXXXXXXXXXXX")) {
			com.vaadin.ui.JavaScript.getCurrent().execute(
				"try {" +
				"    var script = document.createElement('script');" +
				"    script.onload = function() {" +
				"        console.log('Google reCAPTCHA script loaded');" +
				"        grecaptcha.ready(function() {" +
				"            grecaptcha.execute('" + recaptchaSiteKey + "', {action: '" + recaptchaVerificationAction + "'}).then(function(token) {" +
				"                document.getElementsByClassName('" + recaptchaVerificationStyleName + "')[0].value = token;" +
				"                document.getElementsByClassName('" + recaptchaVerificationStyleName + "')[0].dispatchEvent(new Event('change'));" +
				"            });" +
				"        });" +
				"    };" +
				"    script.src = 'https://www.google.com/recaptcha/api.js?render=" + recaptchaSiteKey + "';" +
				"    document.head.appendChild(script);" +
				"} catch (error) {" +
				"    console.error(error);" +
				"}"
			);
		}
	}

	public void show() {
		UI.getCurrent().addWindow(this);

		loadAndExecuteRecaptcha();
	}
}
