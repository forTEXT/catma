package de.catma.ui.modules.main.signup;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.jboss.netty.handler.codec.http.QueryStringEncoder;

import com.google.common.base.Joiner;
import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Signup dialog asks for an email address, and verifies via google recaptcha that no bots 
 * sign up.
 * @author db
 *
 */
@JavaScript({"https://www.google.com/recaptcha/api.js?render=6LenwosUAAAAAKfYcN4ZAGMu1QwfECD2cZPjLoFG"})
public class SignUpDialog extends Window {

	private UserData user = new UserData();
	private GoogleVerificationResult recaptchaResult = new GoogleVerificationResult();

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private final Binder<UserData> userBinder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitlabManagerPrivileged= new GitlabManagerPrivileged();
	
	public SignUpDialog(String caption) {
		super(caption);
		setWidth("400px");
		initComponents();
	}

	private void initComponents() {
		setModal(true);
		
		VerticalLayout content = new VerticalLayout();
		content.addStyleName("spacing");
		content.addStyleName("margin");
		Label description = new Label("Description", ContentMode.HTML);
		description.setValue("Please signup using only your valid email address. <br /> "
				+ "We'll send you an activation link back to your address.<br />"
				+ "After successful activation you'll need to complete your profile with username and password.");
		description.setWidth("100%");
		content.addComponent(description);
		TextField tfEmail = new TextField("Email address");
		tfEmail.setSizeFull();
		tfEmail.setEnabled(false);
		tfEmail.setDescription("Awaiting Google recaptcha verification...");
		content.addComponent(tfEmail);
		
		TextField hiddenVerification = new TextField();
		hiddenVerification.addStyleName("g-recaptcha-response");
		hiddenVerification.addValueChangeListener(event -> 		{
			recaptchaResult = new GoogleRecaptchaVerifier().verify(hiddenVerification.getValue());
			if(recaptchaResult.isSuccess()){
				tfEmail.setEnabled(true);
				tfEmail.setDescription("Please enter your email address");
				com.vaadin.ui.JavaScript.getCurrent().execute("grecaptcha.reset();");
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
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.addStyleName("spacing-left-right");
		buttonPanel.setJustifyContent(JustifyContent.FLEX_END);
		Button btnSignup = new Button("Sign Up");
		
		Button btnCancel = new Button("Cancel");
		
		buttonPanel.addComponent(btnCancel);
		buttonPanel.addComponent(btnSignup);
		
		btnCancel.addClickListener(evt -> this.close());
		
		content.addComponent(hiddenVerification);
		content.addComponent(buttonPanel);
				
		userBinder.forField(tfEmail)
	    .withValidator(new EmailValidator("must be a valid email address"))
	    .withValidator(new AccountAlreadyTakenValidator(gitlabManagerPrivileged))
	    .bind(UserData::getEmail, UserData::setEmail);
		
		btnSignup.addClickListener(click -> {
			
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
				SignupTokenManager tokenManager = new SignupTokenManager();		       
		        String token = HmacUtils.hmacSha256Hex(RepositoryPropertyKey.signup_tokenKey.getValue(), user.getEmail());
		        tokenManager.put( new SignupToken(LocalTime.now().toString(), user.getEmail(), token));
		        QueryStringEncoder qs = new QueryStringEncoder(RepositoryPropertyKey.BaseURL.getValue().trim()+"verify");
		        qs.addParam("token", token);
		        
		        Email email = new SimpleEmail();

		        email.setHostName(RepositoryPropertyKey.MailHost.getValue());
		        email.setSmtpPort(RepositoryPropertyKey.MailPort.getValue(587));
		        if (RepositoryPropertyKey.MailAuthenticationNeeded.getValue(false)) {
		        	email.setAuthenticator(
		        		new DefaultAuthenticator(
		        			RepositoryPropertyKey.MailUser.getValue(), 
		        			RepositoryPropertyKey.MailPass.getValue()));
		        	email.setStartTLSEnabled(true);
		        }
		        email.setFrom(RepositoryPropertyKey.MailFrom.getValue());

		        email.setSubject("CATMA Activation");
		        email.setMsg("In order to verify your account please visit the following link.\n"+qs.toString());
		        email.addTo(user.getEmail(),user.getName());
		        email.send();

		        logger.info("token URL is: "  + qs.toString());
		        Notification.show("To complete your account please click the link that has been sent to your email",
		        		Type.TRAY_NOTIFICATION);
			} catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't create a new user in backend", e);
			}
			this.close();
		});
		setContent(content);
	}

	/**
	 * shows the dialog and registers the javascript callback for recaptcha
	 */
	public void show() {
		UI.getCurrent().addWindow(this);
		com.vaadin.ui.JavaScript.getCurrent().execute(
		  "grecaptcha.ready(function() {"+
		  " grecaptcha.execute('6LenwosUAAAAAKfYcN4ZAGMu1QwfECD2cZPjLoFG', {action: 'SignUpFormCatma'})"+
		  " .then(function(token) {"+
		  "  document.getElementsByClassName('g-recaptcha-response')[0].value = token;"+
		  "  document.getElementsByClassName('g-recaptcha-response')[0].dispatchEvent(new Event('change'));"+
		  " }); "+
		  "}); ");
		  
	}
}
