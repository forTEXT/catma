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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.eventbus.EventBus;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.ClassResource;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ExceptionUtil;

/**
 * @author marco.petris@web.de
 *
 */
public class AuthenticationDialog extends Window implements Handler {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Button btCancel;
	private Button googleLogInLink;
	private String baseUrl;
	private TextField tfUsername;
	private PasswordField pfPassword;
	private Button btLogin;
	private final LoginService loginservice;
	private final InitializationService initService;
	private final EventBus eventBus;
	private final HazelCastService hazelCastService;
	private final Action personalAccessTokenAction = 
			new ShortcutAction("Alt+P", ShortcutAction.KeyCode.P, new int[] { ShortcutAction.ModifierKey.ALT });

	private PasswordField pfPersonalAccessToken;
	
	public AuthenticationDialog(
			String caption, 
			String baseUrl,
			LoginService loginService,
			InitializationService initService,
			HazelCastService hazelCastService,
			EventBus eventBus) { 
		
		super(caption);

		this.baseUrl = baseUrl;
		this.loginservice = loginService;
		this.initService = initService;
		this.hazelCastService = hazelCastService;
		this.eventBus = eventBus;
		
	
		initComponents();
		initActions();
	}
	

	@Override
	public Action[] getActions(Object target, Object sender) {
	    if (sender == AuthenticationDialog.this) {
	        return new Action[] {personalAccessTokenAction};
	    }
	    return null;
	}

	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if (action.equals(personalAccessTokenAction) && sender.equals(AuthenticationDialog.this)) {
			pfPersonalAccessToken.setVisible(!pfPersonalAccessToken.isVisible());
			tfUsername.setVisible(!pfPersonalAccessToken.isVisible());
			pfPassword.setVisible(!pfPersonalAccessToken.isVisible());
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
				Component mainView = initService.newEntryPage(eventBus, loginservice, hazelCastService);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				close();
				
			} catch (IOException e) {
				Notification.show("Login error", "Username or password wrong!", Type.ERROR_MESSAGE);
				String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
				if (message != null && !message.equals("invalid_grant")) {
					logger.log(Level.SEVERE,"login services" , e);
				}
			}
		});
		
		
		googleLogInLink.addClickListener(event -> {
			try {
				UI.getCurrent().getPage().setLocation(createLogInClick(
					CATMAPropertyKey.Google_oauthAuthorizationCodeRequestURL.getValue(),
					CATMAPropertyKey.Google_oauthClientId.getValue(),
					CATMAPropertyKey.Google_oauthClientSecret.getValue())); //$NON-NLS-1$
				close();
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError(
						"Error during authentication!", e);
			}
		});
	}
	
	public String createLogInClick(
			String oauthAuthorizationCodeRequestURL, 
			String oauthClientId,
			String openidRealm) throws UnsupportedEncodingException {
		
		String token = new BigInteger(130, new SecureRandom()).toString(32);

		VaadinSession.getCurrent().setAttribute("OAUTHTOKEN",token);
		
		// state token generation
		Totp totp = new Totp(
				CATMAPropertyKey.otpSecret.getValue()+token, 
				new Clock(Integer.valueOf(CATMAPropertyKey.otpDuration.getValue())));

		// creating the authorization request link 
		StringBuilder authenticationUrlBuilder = new StringBuilder();
		authenticationUrlBuilder.append(
			oauthAuthorizationCodeRequestURL);
		authenticationUrlBuilder.append("?client_id="); //$NON-NLS-1$
		authenticationUrlBuilder.append(oauthClientId);
			
		authenticationUrlBuilder.append("&response_type=code"); //$NON-NLS-1$
		authenticationUrlBuilder.append("&scope=openid%20email"); //$NON-NLS-1$
		authenticationUrlBuilder.append("&redirect_uri="+URLEncoder.encode(baseUrl, "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
		authenticationUrlBuilder.append("&state=" + totp.now()); //$NON-NLS-1$
		authenticationUrlBuilder.append("&openid.realm="+openidRealm); //$NON-NLS-1$
	
		return authenticationUrlBuilder.toString();
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
		
		tfUsername = new TextField("Username or email");
		tfUsername.setWidth("100%");
		
		pfPassword = new PasswordField("Password");
		pfPassword.setWidth("100%");
		
		pfPersonalAccessToken = new PasswordField("Personal Access Token");
		pfPersonalAccessToken.setWidth("100%");
		pfPersonalAccessToken.setVisible(false);
		
		content.addComponent(tfUsername);
		content.addComponent(pfPassword);
		content.addComponent(pfPersonalAccessToken);
		
		HorizontalLayout gOauthPanel = new HorizontalLayout();
		gOauthPanel.setMargin(new MarginInfo(true, false, true, true));
		content.addComponent(gOauthPanel);
		gOauthPanel.addComponent(new Label("or"));
		googleLogInLink = new Button("Log in with your Google account");
		googleLogInLink.setIcon(new ClassResource("module/main/login/resources/google.png")); //$NON-NLS-1$
		googleLogInLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleLogInLink.addStyleName("authdialog-loginlink"); //$NON-NLS-1$
		
		gOauthPanel.addComponent(googleLogInLink);
		
		Label termsOfUse = new Label(
				MessageFormat.format(
					"By logging in you accept the <a target=\"blank\" href=\"{0}\">terms of use</a>!", 
					"http://catma.de/documentation/terms-of-use-privacy-policy/")); //$NON-NLS-1$
		termsOfUse.setContentMode(ContentMode.HTML);
		termsOfUse.setWidth("100%");
		
		content.addComponent(termsOfUse);
		content.setExpandRatio(termsOfUse, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		btLogin = new Button("Login"); 
		btLogin.setClickShortcut(KeyCode.ENTER);
		
		btCancel = new Button("Cancel");

		buttonPanel.addComponent(btLogin);
		buttonPanel.addComponent(btCancel);

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
