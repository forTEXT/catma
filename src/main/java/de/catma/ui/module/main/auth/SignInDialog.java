package de.catma.ui.module.main.auth;

import com.google.common.eventbus.EventBus;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ExternalResource;
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
 * SignInDialog allows users to sign in using one of two options:
 *  - a CATMA account (OAuth authorization code flow against the GitLab backend, where the user enters their credentials)
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
		setWidth("50%");
		setModal(true);

		VerticalLayout content = new VerticalLayout();
		content.setStyleName("signin-dialog");

		regularSignInLayout = new VerticalLayout();
		regularSignInLayout.setMargin(false);

		Panel pnlNotice = new Panel("Heads up: sign-in has changed!");
		pnlNotice.setStyleName("notice-panel");

		VerticalLayout pnlNoticeContent = new VerticalLayout();
		pnlNoticeContent.setStyleName("content");

		Label lblNoticeBody = new Label(
				"You now sign in on GitLab, with your usual CATMA credentials."
		);
		lblNoticeBody.setWidth("100%");

		pnlNoticeContent.addComponent(lblNoticeBody);

		pnlNotice.setContent(pnlNoticeContent);

		HorizontalLayout hlSignInProcessDescriptionAndButton = new HorizontalLayout();
		hlSignInProcessDescriptionAndButton.setWidth("100%");

		Label lblSignInProcessDescription = new Label(
				"CATMA is built on GitLab, which we use to manage your account and data. Click \"Sign In\" and you'll be redirected to GitLab to log in, "
						+ "then brought straight back to CATMA."
		);
		lblSignInProcessDescription.setWidth("100%");
		lblSignInProcessDescription.setStyleName("description-with-button");

		btnRegularSignIn = new Button("Sign In");
		btnRegularSignIn.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		hlSignInProcessDescriptionAndButton.addComponent(lblSignInProcessDescription);
		hlSignInProcessDescriptionAndButton.addComponent(btnRegularSignIn);
		hlSignInProcessDescriptionAndButton.setComponentAlignment(btnRegularSignIn, Alignment.BOTTOM_RIGHT);
		hlSignInProcessDescriptionAndButton.setExpandRatio(lblSignInProcessDescription, 1f);

		regularSignInLayout.addComponent(pnlNotice);
		regularSignInLayout.addComponent(hlSignInProcessDescriptionAndButton);

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

		Label lblPipe = new Label("|");

		Link privacyPolicyLink = new Link(
				"Privacy Policy",
				new ExternalResource(CATMAPropertyKey.PRIVACY_POLICY_URL.getValue())
		);
		privacyPolicyLink.setTargetName("_blank");

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
