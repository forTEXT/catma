/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.module.main.login;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.eventbus.EventBus;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.sqlite.SqliteService;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.main.auth.AuthenticationDialog;
import de.catma.util.ExceptionUtil;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author marco.petris@web.de
 *
 */
public class SignInDialog extends AuthenticationDialog implements Action.Handler {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final LoginService loginservice;
	private final InitializationService initService;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;
	private final EventBus eventBus;

	private final Action personalAccessTokenAction =
			new ShortcutAction("Alt+P", ShortcutAction.KeyCode.P, new int[] { ShortcutAction.ModifierKey.ALT });

	private VerticalLayout userPasswordLoginLayout;
	private TextField tfUsername;
	private PasswordField pfPassword;

	private PasswordField pfPersonalAccessToken;

	private Button googleLogInLink;

	private Button btLogin;
	private Button btCancel;

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
			pfPersonalAccessToken.setVisible(!pfPersonalAccessToken.isVisible());
			userPasswordLoginLayout.setVisible(!userPasswordLoginLayout.isVisible());
		}
	}

	private void initActions() {
		addActionHandler(this);

		btCancel.addClickListener(click -> close());

		btLogin.addClickListener(click -> {
			try {
				if (pfPersonalAccessToken.isVisible()) {
					loginservice.login(pfPersonalAccessToken.getValue());
				}
				else {
					loginservice.login(tfUsername.getValue(), pfPassword.getValue());
				}
				Component mainView = initService.newEntryPage(eventBus, loginservice, hazelCastService, sqliteService);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				close();
			}
			catch (IOException e) {
				Notification.show("Login error", "Username or password wrong!", Notification.Type.ERROR_MESSAGE);
				String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
				if (message != null && !message.equals("invalid_grant")) {
					logger.log(Level.SEVERE, "login services", e);
				}
			}
		});

		googleLogInLink.addClickListener(event -> {
			try {
				UI.getCurrent().getPage().setLocation(getGoogleOauthAuthorisationRequestUrl());
				close();
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Error during authentication!", e);
			}
		});
	}

	@Override
	public void attach() {
		super.attach();
		tfUsername.focus();
	}

	private void initComponents() {
		setModal(true);
		setWidth("50%");
		setHeight("60%");

		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();

		userPasswordLoginLayout = new VerticalLayout();
		userPasswordLoginLayout.setMargin(false);

		tfUsername = new TextField("Username or email");
		tfUsername.setWidth("100%");

		pfPassword = new PasswordField("Password");
		pfPassword.setWidth("100%");

		Link forgotPasswordLink = new Link(
				"Forgot your password?",
				new ExternalResource(CATMAPropertyKey.ResetPasswordURL.getValue(CATMAPropertyKey.ResetPasswordURL.getDefaultValue()))
		);
		forgotPasswordLink.setStyleName("authdialog-forgot-password-link");

		userPasswordLoginLayout.addComponent(tfUsername);
		userPasswordLoginLayout.addComponent(pfPassword);
		userPasswordLoginLayout.addComponent(forgotPasswordLink);
		
		pfPersonalAccessToken = new PasswordField("Personal Access Token");
		pfPersonalAccessToken.setWidth("100%");
		pfPersonalAccessToken.setVisible(false);

		content.addComponent(userPasswordLoginLayout);
		content.addComponent(pfPersonalAccessToken);

		HorizontalLayout gOauthPanel = new HorizontalLayout();
		gOauthPanel.setMargin(new MarginInfo(true, false, true, false));

		Label lblOr = new Label("or");
		gOauthPanel.addComponent(lblOr);

		googleLogInLink = new Button();
		googleLogInLink.setIcon(new ThemeResource("img/google_buttons/btn_google_light_normal_sign_in.svg")); //$NON-NLS-1$
		googleLogInLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleLogInLink.addStyleName("authdialog-google-login-link"); //$NON-NLS-1$
		gOauthPanel.addComponent(googleLogInLink);

		gOauthPanel.setComponentAlignment(lblOr, Alignment.MIDDLE_LEFT);
		gOauthPanel.setComponentAlignment(googleLogInLink, Alignment.MIDDLE_LEFT);

		content.addComponent(gOauthPanel);
		content.setExpandRatio(gOauthPanel, 1f);

		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");

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

		btLogin = new Button("Sign in");
		btLogin.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		btCancel = new Button("Cancel");

		buttonPanel.addComponent(termsOfUseLink);
		buttonPanel.addComponent(lblPipe);
		buttonPanel.addComponent(privacyPolicyLink);
		buttonPanel.addComponent(btLogin);
		buttonPanel.addComponent(btCancel);

		buttonPanel.setComponentAlignment(termsOfUseLink, Alignment.BOTTOM_LEFT);
		buttonPanel.setComponentAlignment(lblPipe, Alignment.BOTTOM_LEFT);
		buttonPanel.setComponentAlignment(privacyPolicyLink, Alignment.BOTTOM_LEFT);
		buttonPanel.setComponentAlignment(btCancel, Alignment.BOTTOM_RIGHT);
		buttonPanel.setComponentAlignment(btLogin, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btLogin, 1f);

		content.addComponent(buttonPanel);

		setContent(content);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
