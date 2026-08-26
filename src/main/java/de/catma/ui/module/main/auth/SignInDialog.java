package de.catma.ui.module.main.auth;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.eventbus.EventBus;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.sqlite.SqliteService;
import de.catma.ui.CatmaApplication;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;

import java.io.IOException;

/**
 * SignInDialog allows users to sign in using one of three options:
 *  - a CATMA account (OAuth authorization code flow against the GitLab backend, where the user enters their credentials)
 *  - Google (OpenID Connect)
 *  - personal access token (hidden - accessed via keyboard shortcut Alt+P)
 *
 */
public class SignInDialog extends AuthenticationDialog implements Action.Handler {
	private final LoginService loginservice;
	private final InitializationService initService;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;
	private final EventBus eventBus;

	private final Action personalAccessTokenAction =
			new ShortcutAction("Alt+P", ShortcutAction.KeyCode.P, new int[] { ShortcutAction.ModifierKey.ALT });

	private VerticalLayout regularSignInLayout;
	private Button btnRegularSignIn;
	private Button googleSignInLink;

	private VerticalLayout patSignInLayout;
	private PasswordField pfPersonalAccessToken;
	private Button btnPatSignIn;

	public SignInDialog(
			String caption,
			LoginService loginService,
			InitializationService initService,
			HazelCastService hazelCastService,
			SqliteService sqliteService,
			EventBus eventBus
	) {
		super(caption);

		this.loginservice = loginService;
		this.initService = initService;
		this.hazelCastService = hazelCastService;
		this.sqliteService = sqliteService;
		this.eventBus = eventBus;

		initComponents();
		initActions();
	}

	@Override
	public Action[] getActions(Object target, Object sender) {
		if (sender == SignInDialog.this) {
			return new Action[] { personalAccessTokenAction };
		}
		return null;
	}

	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if (action.equals(personalAccessTokenAction) && sender.equals(SignInDialog.this)) {
			regularSignInLayout.setVisible(!regularSignInLayout.isVisible());
			patSignInLayout.setVisible(!patSignInLayout.isVisible());

			if (regularSignInLayout.isVisible()) {
				btnPatSignIn.removeClickShortcut();
				btnRegularSignIn.focus();
				btnRegularSignIn.setClickShortcut(ShortcutAction.KeyCode.ENTER);
			}
			else if (patSignInLayout.isVisible()) {
				btnRegularSignIn.removeClickShortcut();
				pfPersonalAccessToken.focus();
				btnPatSignIn.setClickShortcut(ShortcutAction.KeyCode.ENTER);
			}
		}
	}

	private void initMainView() {
		Component mainView = initService.newEntryPage(eventBus, loginservice, hazelCastService, sqliteService);
		UI.getCurrent().setContent(mainView);
		eventBus.post(new RouteToDashboardEvent());
		close();
		((CatmaApplication)UI.getCurrent()).handleRequestToken();
	}

	private void initActions() {
		addActionHandler(this);

		// this redirects to the GitLab backend, where the user enters their credentials - we return via the OAuth callback
		// (see CatmaApplication.handleRequestOauth), so there is nothing else to do here
		btnRegularSignIn.addClickListener(this::gitLabLinkClickListener);

		googleSignInLink.addClickListener(this::googleLinkClickListener);

		btnPatSignIn.addClickListener(event -> {
			try {
				loginservice.login(pfPersonalAccessToken.getValue());
				initMainView();
			}
			catch (IOException e) {
				// TODO: distinguish between different types of exception, don't assume...
				Notification.show("Login error", "Invalid token!", Notification.Type.ERROR_MESSAGE);
			}
		});
	}

	private void initComponents() {
		setWidth("60%");
		setModal(true);

		VerticalLayout content = new VerticalLayout();
		content.setWidthFull();
		content.setStyleName("signin-dialog");

		regularSignInLayout = new VerticalLayout();
		regularSignInLayout.setMargin(false);

		Label lblChoice = new Label("Please choose one of the options below:");
		lblChoice.setWidth("100%");

		Panel pnlEmail = new Panel("Option 1: CATMA Account");
		pnlEmail.setStyleName("email-panel");
		VerticalLayout pnlEmailContent = new VerticalLayout();

		Label lblRegularSignIn = new Label(
				"You will be redirected to CATMA's GitLab backend, where you can sign in with your username or email address and your password.",
				ContentMode.HTML
		);
		lblRegularSignIn.setWidth("100%");

		HorizontalLayout hlForgotPasswordAndButton = new HorizontalLayout();
		hlForgotPasswordAndButton.setWidth("100%");

		Link forgotPasswordLink = new Link(
				"Forgot your password?",
				new ExternalResource(CATMAPropertyKey.RESET_PASSWORD_URL.getValue())
		);
		forgotPasswordLink.setStyleName("authdialog-forgot-password-link");

		btnRegularSignIn = new Button("Sign In");
		btnRegularSignIn.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		hlForgotPasswordAndButton.addComponent(forgotPasswordLink);
		hlForgotPasswordAndButton.addComponent(btnRegularSignIn);
		hlForgotPasswordAndButton.setComponentAlignment(btnRegularSignIn, Alignment.BOTTOM_RIGHT);
		hlForgotPasswordAndButton.setExpandRatio(forgotPasswordLink, 1f);

		pnlEmailContent.addComponent(lblRegularSignIn);
		pnlEmailContent.addComponent(hlForgotPasswordAndButton);
		pnlEmail.setContent(pnlEmailContent);

		Panel pnlGoogle = new Panel("Option 2: Google Account");
		pnlGoogle.setStyleName("google-panel");
		VerticalLayout pnlGoogleContent = new VerticalLayout();

		googleSignInLink = new Button();
		googleSignInLink.setIcon(new ThemeResource("img/google_buttons/btn_google_light_normal_sign_in.svg"));
		googleSignInLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleSignInLink.addStyleName("authdialog-google-login-link");

		pnlGoogleContent.addComponent(googleSignInLink);
		pnlGoogle.setContent(pnlGoogleContent);

		regularSignInLayout.addComponent(lblChoice);
		regularSignInLayout.addComponent(pnlEmail);
		regularSignInLayout.addComponent(pnlGoogle);

		patSignInLayout = new VerticalLayout();
		patSignInLayout.setMargin(false);
		patSignInLayout.setVisible(false);

		pfPersonalAccessToken = new PasswordField("Personal Access Token");
		pfPersonalAccessToken.setWidth("100%");

		btnPatSignIn = new Button("Sign In");

		patSignInLayout.addComponent(pfPersonalAccessToken);
		patSignInLayout.addComponent(btnPatSignIn);
		patSignInLayout.setComponentAlignment(btnPatSignIn, Alignment.BOTTOM_RIGHT);

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

		content.addComponent(regularSignInLayout);
		content.addComponent(patSignInLayout);
		content.addComponent(hlLinks);

		setContent(content);
	}

	@Override
	public void attach() {
		super.attach();
		btnRegularSignIn.focus();
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
