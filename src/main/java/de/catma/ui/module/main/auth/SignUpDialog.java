package de.catma.ui.module.main.auth;

import java.time.LocalTime;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;
import io.netty.handler.codec.http.QueryStringEncoder;

/**
 * Signup dialog asks for an email address, and verifies via google recaptcha that no bots 
 * sign up.
 * @author db
 *
 */
public class SignUpDialog extends AuthenticationDialog {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final UserData userData = new UserData();

	private final Binder<UserData> userDataBinder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitManagerPrivileged = new GitlabManagerPrivileged();

	private final String recaptchaVerificationStyleName = "g-recaptcha-response";
	private final String recaptchaVerificationAction = "CatmaSignUpForm";
	private final String recaptchaSiteKey = CATMAPropertyKey.Google_recaptchaSiteKey.getValue();

	private GoogleVerificationResult recaptchaResult = new GoogleVerificationResult();

	private TextField tfEmail;
	private Button btnSignup;
	private TextField hiddenVerification;

	private Button googleSignUpLink;

	public SignUpDialog(String caption) {
		super(caption);
		initComponents();
		initActions();
	}

	private boolean isRecaptchaVerified() {
		return recaptchaResult.isSuccess() && recaptchaResult.getScore() >= 0.5f && recaptchaResult.getAction().equals(recaptchaVerificationAction);
	}

	// TODO: this shouldn't be in the UI code
	// TODO: document how this interacts with handleRequestToken in CatmaApplication
	private void generateSignupTokenAndSendVerificationEmail() throws EmailException {
		String token = HmacUtils.hmacSha256Hex(CATMAPropertyKey.signup_tokenKey.getValue(), userData.getEmail());

		SignupTokenManager tokenManager = new SignupTokenManager();
		tokenManager.put(new SignupToken(LocalTime.now().toString(), userData.getEmail(), token));

		QueryStringEncoder qs = new QueryStringEncoder(CATMAPropertyKey.BaseURL.getValue().trim() + "verify");
		qs.addParam("token", token);

		Email email = new SimpleEmail();
		email.setHostName(CATMAPropertyKey.MailHost.getValue("localhost"));
		email.setSmtpPort(CATMAPropertyKey.MailPort.getValue(587));

		if (CATMAPropertyKey.MailAuthenticationNeeded.getValue(false)) {
			email.setAuthenticator(
					new DefaultAuthenticator(
							CATMAPropertyKey.MailUser.getValue(),
							CATMAPropertyKey.MailPass.getValue()
					)
			);
			email.setStartTLSEnabled(true);
		}

		email.setFrom(CATMAPropertyKey.MailFrom.getValue("support@catma.de"));
		email.setSubject("CATMA Email Verification");
		email.setMsg("Please visit the following link in order to verify your email address and complete your sign up:\n" + qs);
		email.addTo(userData.getEmail());
		email.send();

		logger.info(
				String.format("Generated a new signup token for %s, the full verification URL is: %s", userData.getEmail(), qs)
		);
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

				Notification completeSignupNotification = new Notification(
						"To complete your sign up, please click the link in the verification email!",
						Notification.Type.WARNING_MESSAGE
				);
				completeSignupNotification.setDelayMsec(-1);
				completeSignupNotification.show(Page.getCurrent());
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't send verification email", e);
			}

			this.close();
		});

		googleSignUpLink.addClickListener(event -> {
			try {
				UI.getCurrent().getPage().setLocation(getGoogleOauthAuthorisationRequestUrl());
				close();
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Error during authentication!", e);
			}
		});
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
		btnSignup.setEnabled(false);
		btnSignup.setDescription("Please wait a moment while we verify that you're not a bot...");
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
				new ExternalResource(CATMAPropertyKey.TermsOfUseURL.getValue(CATMAPropertyKey.TermsOfUseURL.getDefaultValue()))
		);
		termsOfUseLink.setTargetName("_blank");
		termsOfUseLink.setStyleName("authdialog-tou-link");

		Label lblPipe = new Label("|");

		Link privacyPolicyLink = new Link(
				"Privacy Policy",
				new ExternalResource(CATMAPropertyKey.PrivacyPolicyURL.getValue(CATMAPropertyKey.PrivacyPolicyURL.getDefaultValue()))
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

	public void show() {
		UI.getCurrent().addWindow(this);

		loadAndExecuteRecaptcha();
	}
}
