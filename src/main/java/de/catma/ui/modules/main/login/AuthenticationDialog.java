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
package de.catma.ui.modules.main.login;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.appreciated.material.MaterialTheme;
import com.google.common.eventbus.EventBus;
import com.vaadin.server.ClassResource;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.repository.Messages;

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

	
	private static class AuthenticationRequestHandler implements RequestHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private String returnURL;
		private Window dialogWindow;
		private UI ui;
		private String token;
		private String oauthAccessTokenRequestURL;
		private String oauthClientId;
		private String oauthClientSecret;
		private final EventBus eventBus = VaadinSession.getCurrent().getAttribute(EventBus.class);
		private final LoginService loginservice;
		private final InitializationService initService;
		
		public AuthenticationRequestHandler(
				UI ui, // UI.getCurrent() is not available during request handling, therefore we pass in the UI
				LoginService loginservice,
				InitializationService initService,
				String returnURL, 
				Window dialogWindow, 
				String token,
				String oauthAccessTokenRequestURL,
				String oauthClientId,
				String oauthClientSecret) {
			super();
			this.ui = ui;
			this.returnURL = returnURL;
			this.dialogWindow = dialogWindow;
			this.token = token;
			this.oauthAccessTokenRequestURL = oauthAccessTokenRequestURL;
			this.oauthClientId = oauthClientId;
			this.oauthClientSecret = oauthClientSecret;
			this.loginservice = loginservice;
			this.initService = initService;
		}
			
		@Override
		public boolean handleRequest(VaadinSession session,
				VaadinRequest request, VaadinResponse response)
				throws IOException {

			// this handles the answer to the authorization request
			try {
				
				// clean up
				VaadinSession.getCurrent().removeRequestHandler(this);
				
				if (ui == null) {
					throw new NullPointerException(Messages.getString("AuthenticationDialog.UIUnavailable")); //$NON-NLS-1$
				}
				
				ui.removeWindow(dialogWindow);
				
				dialogWindow = null;
				
				
				// extract answer
				String authorizationCode = request.getParameter("code"); //$NON-NLS-1$

				String state = request.getParameter("state"); //$NON-NLS-1$

				String error = request.getParameter("error"); //$NON-NLS-1$

				// do we have a authorization request error?
				if (error == null) {
					// no, so we validate the state token
					Totp totp = new Totp(
							RepositoryPropertyKey.otpSecret.getValue()+token, 
							new Clock(Integer.valueOf(
								RepositoryPropertyKey.otpDuration.getValue())));
					if (!totp.verify(state)) {
						error = "state token verification failed"; //$NON-NLS-1$
					}
				}
				
				// state token get validation success?	
				if (error == null) {
					CloseableHttpClient httpclient = HttpClients.createDefault();
					HttpPost httpPost = 
						new HttpPost(oauthAccessTokenRequestURL);
					List <NameValuePair> data = new ArrayList <NameValuePair>();
					data.add(new BasicNameValuePair("code", authorizationCode)); //$NON-NLS-1$
					data.add(new BasicNameValuePair("grant_type", "authorization_code")); //$NON-NLS-1$ //$NON-NLS-2$
					data.add(new BasicNameValuePair(
						"client_id", oauthClientId)); //$NON-NLS-1$
					data.add(new BasicNameValuePair(
						"client_secret", oauthClientSecret)); //$NON-NLS-1$
					data.add(new BasicNameValuePair("redirect_uri", returnURL)); //$NON-NLS-1$
					httpPost.setEntity(new UrlEncodedFormEntity(data));
					CloseableHttpResponse tokenRequestResponse = httpclient.execute(httpPost);
					HttpEntity entity = tokenRequestResponse.getEntity();
					InputStream content = entity.getContent();
					ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
					IOUtils.copy(content, bodyBuffer);
					
					logger.info("access token request result: " + bodyBuffer.toString("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
					
					ObjectMapper mapper = new ObjectMapper();

					ObjectNode accessTokenResponseJSon = 
							mapper.readValue(bodyBuffer.toString(), ObjectNode.class);

					// we're actually not interested in the access token 
					// but we want the email information from the id token
					String idToken = accessTokenResponseJSon.get("id_token").asText(); //$NON-NLS-1$
					
					String[] pieces = idToken.split("\\."); //$NON-NLS-1$
					// we skip the header and go ahead with the payload
					String payload = pieces[1];
		
					String decodedPayload = 
							new String(Base64.decodeBase64(payload), "UTF-8"); //$NON-NLS-1$
					ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
					
					logger.info("decodedPayload: " + decodedPayload); //$NON-NLS-1$
					
					// finally the email address
					// String email = payloadJson.get("email").asText(); //$NON-NLS-1$

					String identifier = payloadJson.get("sub").asText();
					String name = payloadJson.get("name") == null ? identifier : payloadJson.get("name").asText();
					String email = payloadJson.get("email").asText();
					String provider = "google.com";
					loginservice.loggedInFromThirdParty(identifier, provider, email, name);

					Component mainView = initService.newEntryPage(loginservice);
					ui.setContent(mainView);
					ui.getPage().replaceState(RepositoryPropertyKey.BaseURL.getValue());
					eventBus.post(new RouteToDashboardEvent());
		                
	                return false;
				}
				else {
	                logger.info("authentication failure: " + error); //$NON-NLS-1$
					new Notification(
                        Messages.getString("AuthenticationDialog.authFailureTitle"), //$NON-NLS-1$
                        Messages.getString("AuthenticationDialog.authFailureMessage"), //$NON-NLS-1$
                        Type.ERROR_MESSAGE).show(ui.getPage());

					return false;
				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
				((ErrorHandler)ui).showAndLogError(
						Messages.getString("AuthenticationDialog.errorOpeningRepo"), e); //$NON-NLS-1$
			}
			
			ui.close();
			
			return false;
		}
	}
	
	private Button btCancel;
	private Button googleLogInLink;
	private String baseUrl;
	private TextField tfUsername;
	private PasswordField pfPassword;
	private Button btLogin;
	private final LoginService loginservice;
	private final InitializationService initService;
	private final EventBus eventBus = VaadinSession.getCurrent().getAttribute(EventBus.class);

	public AuthenticationDialog(
			String caption, 
			String baseUrl,
			LoginService loginService,
			InitializationService initService
			) { 
		super(caption);
		setModal(true);
		this.baseUrl = baseUrl;
		this.loginservice = loginService;
		this.initService = initService;
		
	
		initComponents();
		initActions();
	}


	private void initActions() {
		btCancel.addClickListener(click -> close());			

		btLogin.addClickListener(click -> {
			try {
				
				loginservice.login(tfUsername.getValue(), pfPassword.getValue());
				Component mainView = initService.newEntryPage(loginservice);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				close();
				
			} catch (IOException e) {
				Notification.show("Login error", "username or password wrong", Type.ERROR_MESSAGE);
				logger.log(Level.SEVERE,"login services" , e);
			}
		});
		
		
		googleLogInLink.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					UI.getCurrent().getPage().setLocation(createLogInClick(
						UI.getCurrent(), 
						RepositoryPropertyKey.Google_oauthAuthorizationCodeRequestURL.getValue(),
						RepositoryPropertyKey.Google_oauthAccessTokenRequestURL.getValue(),
						RepositoryPropertyKey.Google_oauthClientId.getValue(),
						RepositoryPropertyKey.Google_oauthClientSecret.getValue(),
						URLEncoder.encode(baseUrl, "UTF-8"))); //$NON-NLS-1$
				}
				catch (Exception e) {
					((ErrorHandler)UI.getCurrent()).showAndLogError(Messages.getString("AuthenticationDialog.errorDuringAuth"), e); //$NON-NLS-1$
				}
			}
		});
	}
	
	public String createLogInClick(
			UI ui, 
			String oauthAuthorizationCodeRequestURL, 
			String oauthAccessTokenRequestURL,
			String oauthClientId,
			String oauthClientSecret,
			String openidRealm) throws UnsupportedEncodingException {
		
		String token = new BigInteger(130, new SecureRandom()).toString(32);

		// state token generation
		Totp totp = new Totp(
				RepositoryPropertyKey.otpSecret.getValue()+token, 
				new Clock(Integer.valueOf(RepositoryPropertyKey.otpDuration.getValue())));

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
		
		
		final AuthenticationRequestHandler authenticationRequestHandler =
				new AuthenticationRequestHandler(
						ui,
						loginservice,
						initService,
						baseUrl, 
						this,
						token,
						oauthAccessTokenRequestURL,
						oauthClientId,
						oauthClientSecret);
		
		
		VaadinSession.getCurrent().addRequestHandler(authenticationRequestHandler);
		
		return authenticationUrlBuilder.toString();
	}


	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.addStyleName("spacing");
		content.addStyleName("margin");
		
		tfUsername = new TextField("Username");
		tfUsername.setSizeFull();
		pfPassword = new PasswordField("Password");
		pfPassword.setSizeFull();
		
		
		content.addComponent(tfUsername);
		content.addComponent(pfPassword);
		
		googleLogInLink = new Button(Messages.getString("AuthenticationDialog.logInWithGoogleAccount")); //$NON-NLS-1$
		googleLogInLink.setIcon(new ClassResource(Messages.getString("AuthenticationDialog.48"))); //$NON-NLS-1$
		googleLogInLink.setStyleName(MaterialTheme.BUTTON_LINK);
		googleLogInLink.addStyleName("authdialog-loginlink"); //$NON-NLS-1$
		
		content.addComponent(googleLogInLink);
		
		Label termsOfUse = new Label(
				MessageFormat.format(
					Messages.getString("AuthenticationDialog.termsOfUse"), //$NON-NLS-1$ 
					"http://www.catma.de/termsofuse")); //$NON-NLS-1$
		termsOfUse.setContentMode(ContentMode.HTML);
		termsOfUse.setSizeFull();
		
		content.addComponent(termsOfUse);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.addStyleName("spacing-left-right");
		buttonPanel.setJustifyContent(JustifyContent.FLEX_END);

		btLogin = new Button("Login"); 
		btCancel = new Button(Messages.getString("AuthenticationDialog.Cancel")); //$NON-NLS-1$

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
