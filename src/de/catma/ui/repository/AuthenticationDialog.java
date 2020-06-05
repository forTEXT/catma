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
package de.catma.ui.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.vaadin.server.ClassResource;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.BaseTheme;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.RepositoryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.SessionKey;
import de.catma.user.UserProperty;

/**
 * Authentication dialog for OpenID Connect authentication with Google.
 * Based on: 
 * https://developers.google.com/accounts/docs/OpenIDConnect
 * 
 * @author marco.petris@web.de
 *
 */
public class AuthenticationDialog extends VerticalLayout {
	
	private static class AuthenticationRequestHandler implements RequestHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private String returnURL;
		private RepositoryReference repositoryReference;
		private RepositoryListView repositoryListView;
		private Window dialogWindow;
		private UI ui;
		private String token;
		private String oauthAccessTokenRequestURL;
		private String oauthClientId;
		private String oauthClientSecret;

		public AuthenticationRequestHandler(
				UI ui, // UI.getCurrent() is not available during request handling, therefore we pass in the UI
				String returnURL, 
				RepositoryReference repositoryReference,
				RepositoryListView repositoryListView, Window dialogWindow, 
				String token,
				String oauthAccessTokenRequestURL,
				String oauthClientId,
				String oauthClientSecret) {
			super();
			this.ui = ui;
			this.returnURL = returnURL;
			this.repositoryReference = repositoryReference;
			this.repositoryListView = repositoryListView;
			this.dialogWindow = dialogWindow;
			this.token = token;
			this.oauthAccessTokenRequestURL = oauthAccessTokenRequestURL;
			this.oauthClientId = oauthClientId;
			this.oauthClientSecret = oauthClientSecret;
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
					String email = payloadJson.get("email").asText(); //$NON-NLS-1$

					// construct CATMA user identification
					Map<String, String> userIdentification = 
							new HashMap<String, String>();
					
					logger.info("retrieved email: " + email); //$NON-NLS-1$
					
	                userIdentification.put(
							UserProperty.identifier.name(), email);
	                userIdentification.put(
	                		UserProperty.email.name(), email);
	                userIdentification.put(
	                		UserProperty.name.name(), email);

	                logger.info("opening repository for user: " + email); //$NON-NLS-1$

	                repositoryListView.open(
	                	(CatmaApplication) ui, 
	                	repositoryReference, 
	                	userIdentification);
	                
	                VaadinSession.getCurrent().setAttribute(SessionKey.USER.name(), userIdentification);
	                
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
				((CatmaApplication)ui).showAndLogError(
						Messages.getString("AuthenticationDialog.errorOpeningRepo"), e); //$NON-NLS-1$
			}
			
			ui.close();
			
			return false;
		}
	}
	
	private Window dialogWindow;
	private String caption;
	private Button btCancel;
	private RepositoryReference repositoryReference;
	private RepositoryListView repositoryListView;
	private Button catmaLogInLink;
	private Button googleLogInLink;
	private String baseUrl;
	
	public AuthenticationDialog(
			String caption, RepositoryReference repositoryReference, 
			RepositoryListView repositoryListView, String baseUrl) {
		this.caption = caption;
		this.repositoryReference = repositoryReference;
		this.repositoryListView = repositoryListView;
		this.baseUrl = baseUrl;
		initComponents();
		initActions();
	}


	private void initActions() {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(dialogWindow);
			}
		});
		
		catmaLogInLink.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					UI.getCurrent().getPage().setLocation(createLogInClick(
						UI.getCurrent(), 
						RepositoryPropertyKey.CATMA_oauthAuthorizationCodeRequestURL.getValue(),
						RepositoryPropertyKey.CATMA_oauthAccessTokenRequestURL.getValue(),
						RepositoryPropertyKey.CATMA_oauthClientId.getValue(),
						RepositoryPropertyKey.CATMA_oauthClientSecret.getValue(),
						URLEncoder.encode("/", "UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (Exception e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("AuthenticationDialog.errorDuringAuth"), e); //$NON-NLS-1$
				}
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
					((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("AuthenticationDialog.errorDuringAuth"), e); //$NON-NLS-1$
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
						baseUrl, 
						repositoryReference,
						repositoryListView, 
						dialogWindow,
						token,
						oauthAccessTokenRequestURL,
						oauthClientId,
						oauthClientSecret);
		
		
		VaadinSession.getCurrent().addRequestHandler(authenticationRequestHandler);
		
		return authenticationUrlBuilder.toString();
	}


	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		dialogWindow = new Window(caption, content);
		dialogWindow.setModal(true);
		
		catmaLogInLink = new Button(Messages.getString("AuthenticationDialog.logInWithCATMAAccount")); //$NON-NLS-1$
		catmaLogInLink.setIcon(new ClassResource("repository/resources/catma.png")); //$NON-NLS-1$
		catmaLogInLink.setStyleName(BaseTheme.BUTTON_LINK);
		catmaLogInLink.addStyleName("authdialog-loginlink"); //$NON-NLS-1$
		addComponent(catmaLogInLink);
		
//		Link catmaCreateAccountLink = 
//			new Link(
//				Messages.getString("AuthenticationDialog.createCATMAaccount"),  //$NON-NLS-1$
//				new ExternalResource("https://auth.catma.de/openam/XUI/#register/")); //$NON-NLS-1$
//		catmaCreateAccountLink.setIcon(
//				new ClassResource("repository/resources/catma.png")); //$NON-NLS-1$
		
		Button catmaCreateAccountLink = new Button(Messages.getString("AuthenticationDialog.createCATMAaccount"));  //$NON-NLS-1$
		catmaCreateAccountLink.addClickListener(
			clickEvent -> Notification.show(
				"Info", 
				"Account creation for CATMA 5 has been deactivated. For new projects please use CATMA 6! "
				+ "If for whatever reason you still need access to CATMA 5 please get in contact with us directly via catma-support@catma.de",
				Type.HUMANIZED_MESSAGE));
		addComponent(catmaCreateAccountLink);

		
		googleLogInLink = new Button(Messages.getString("AuthenticationDialog.logInWithGoogleAccount")); //$NON-NLS-1$
		googleLogInLink.setIcon(new ClassResource(Messages.getString("AuthenticationDialog.48"))); //$NON-NLS-1$
		googleLogInLink.setStyleName(BaseTheme.BUTTON_LINK);
		googleLogInLink.addStyleName("authdialog-loginlink"); //$NON-NLS-1$
		addComponent(googleLogInLink);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btCancel = new Button(Messages.getString("AuthenticationDialog.Cancel")); //$NON-NLS-1$
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		content.addComponent(this);
		
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		UI.getCurrent().addWindow(dialogWindow);
	}
	
	public void show() {
		show("400px"); //$NON-NLS-1$
	}
}
