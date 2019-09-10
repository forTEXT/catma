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
import com.vaadin.server.ClassResource;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Authentication dialog for OpenID Connect authentication with Google.
 * Based on: 
 * https://developers.google.com/accounts/docs/OpenIDConnect
 * 
 * @author marco.petris@web.de
 *
 */
public class AuthenticationDialog extends Window {
	
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


	
	public AuthenticationDialog(
			String caption, 
			String baseUrl,
			LoginService loginService,
			InitializationService initService,
			HazelCastService hazelCastService,
			EventBus eventBus
			) { 
		super(caption);
		setModal(true);
		this.baseUrl = baseUrl;
		this.loginservice = loginService;
		this.initService = initService;
		this.hazelCastService = hazelCastService;
		this.eventBus = eventBus;
		
	
		initComponents();
		initActions();
	}


	private void initActions() {
		btCancel.addClickListener(click -> close());			

		btLogin.addClickListener(click -> {
			try {
				
				loginservice.login(tfUsername.getValue(), pfPassword.getValue());
				Component mainView = initService.newEntryPage(loginservice, hazelCastService);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				close();
				
			} catch (IOException e) {
				Notification.show("Login error", "username or password wrong", Type.ERROR_MESSAGE);
				logger.log(Level.SEVERE,"login services" , e);
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


	private void initComponents() {
		VerticalFlexLayout content = new VerticalFlexLayout();
		content.addStyleName("spacing");
		content.addStyleName("margin");
		
		tfUsername = new TextField("Username");
		tfUsername.setSizeFull();
		pfPassword = new PasswordField("Password");
		pfPassword.setSizeFull();
		
		
		content.addComponent(tfUsername);
		content.addComponent(pfPassword);
		
		googleLogInLink = new Button("Log in with your Google account");
		googleLogInLink.setIcon(new ClassResource("module/main/login/resources/google.png")); //$NON-NLS-1$
		googleLogInLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleLogInLink.addStyleName("authdialog-loginlink"); //$NON-NLS-1$
		
		content.addComponent(googleLogInLink);
		
		Label termsOfUse = new Label(
				MessageFormat.format(
					"By logging in you accept the <a target=\"blank\" href=\"{0}\">terms of use</a>!", 
					"http://catma.de/documentation/terms-of-use-privacy-policy/")); //$NON-NLS-1$
		termsOfUse.setContentMode(ContentMode.HTML);
		termsOfUse.setSizeFull();
		
		content.addComponent(termsOfUse);
		
		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout();
		buttonPanel.addStyleName("spacing-left-right");
		buttonPanel.setJustifyContent(JustifyContent.FLEX_END);

		btLogin = new Button("Login"); 
		btCancel = new Button("Cancel");

		buttonPanel.addComponent(btCancel);
		buttonPanel.addComponent(btLogin);
		
		content.addComponent(buttonPanel);
		
		setContent(content);
	}
	
	public void show(String dialogWidth) {
		UI.getCurrent().addWindow(this);
	}
	
	public void show() {
		show("400px"); //$NON-NLS-1$
	}
}
