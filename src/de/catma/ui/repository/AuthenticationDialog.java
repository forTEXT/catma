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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.vaadin.server.ClassResource;
import com.vaadin.server.DownloadStream;
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

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.RepositoryReference;
import de.catma.ui.CatmaApplication;
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

		public AuthenticationRequestHandler(
				UI ui, // UI.getCurrent() is not available during request handling, therefore we pass in the UI
				String returnURL, 
				RepositoryReference repositoryReference,
				RepositoryListView repositoryListView, Window dialogWindow, String token) {
			super();
			this.ui = ui;
			this.returnURL = returnURL;
			this.repositoryReference = repositoryReference;
			this.repositoryListView = repositoryListView;
			this.dialogWindow = dialogWindow;
			this.token = token;
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
					throw new NullPointerException("UI not available!");
				}
				
				ui.removeWindow(dialogWindow);
				
				dialogWindow = null;
				
				
				// extract answer
				String authorizationCode = request.getParameter("code");

				String state = request.getParameter("state");

				String error = request.getParameter("error");

				// do we have a authorization request error?
				if (error == null) {
					// no, so we validate the state token
					Totp totp = new Totp(
							RepositoryPropertyKey.otpSecret.getValue()+token, 
							new Clock(Integer.valueOf(
								RepositoryPropertyKey.otpDuration.getValue())));
					if (!totp.verify(state)) {
						error = "state token verification failed";
					}
				}
				
				// state token get validation success?	
				if (error == null) {
					CloseableHttpClient httpclient = HttpClients.createDefault();
					HttpPost httpPost = 
						new HttpPost(RepositoryPropertyKey.oauthAccessTokenRequestURL.getValue());
					List <NameValuePair> data = new ArrayList <NameValuePair>();
					data.add(new BasicNameValuePair("code", authorizationCode));
					data.add(new BasicNameValuePair("grant_type", "authorization_code"));
					data.add(new BasicNameValuePair(
						"client_id", RepositoryPropertyKey.oauthClientId.getValue()));
					data.add(new BasicNameValuePair(
						"client_secret", RepositoryPropertyKey.oauthClientSecret.getValue()));
					data.add(new BasicNameValuePair("redirect_uri", returnURL));
					httpPost.setEntity(new UrlEncodedFormEntity(data));
					CloseableHttpResponse tokenRequestResponse = httpclient.execute(httpPost);
					HttpEntity entity = tokenRequestResponse.getEntity();
					InputStream content = entity.getContent();
					ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
					IOUtils.copy(content, bodyBuffer);
					ObjectMapper mapper = new ObjectMapper();

					ObjectNode accessTokenResponseJSon = 
							mapper.readValue(bodyBuffer.toString(), ObjectNode.class);

					// we're actually not interested in the access token 
					// but we want the email information from the id token
					String idToken = accessTokenResponseJSon.get("id_token").asText();
					
					String[] pieces = idToken.split("\\.");
					// we skip the header and go ahead with the payload
					String payload = pieces[1];
		
					String decodedPayload = 
							new String(Base64.decodeBase64(payload), "UTF-8");
					ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
					
					logger.info("decodedPayload: " + decodedPayload);
					
					// finally the email address
					String email = payloadJson.get("email").asText();

					// construct CATMA user identification
					Map<String, String> userIdentification = 
							new HashMap<String, String>();
					
					logger.info("retrieved email: " + email);
					
	                userIdentification.put(
							UserProperty.identifier.name(), email);
	                userIdentification.put(
	                		UserProperty.email.name(), email);
	                userIdentification.put(
	                		UserProperty.name.name(), email);

	                logger.info("opening repository for user: " + email);

	                repositoryListView.open(repositoryReference, userIdentification);
	
	                new DownloadStream(
                		ui.getPage().getLocation().toURL().openStream(), 
                		"text/html", "CATMA " + RepositoryPropertyKey.version.getValue()
	                ).writeResponse(request, response);
	                return true;
				}
				else {
	                logger.info("authentication failure: " + error);
					new Notification(
                        "Authentication failure",
                        "The authentication failed, you are not " +
                        "allowed to access this repository!",
                        Type.ERROR_MESSAGE).show(ui.getPage());
	                new DownloadStream(
                		ui.getPage().getLocation().toURL().openStream(), 
                		"text/html", 
                		"CATMA " + RepositoryPropertyKey.version.getValue()
	                ).writeResponse(request, response);
	                return true;
				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
				((CatmaApplication)ui).showAndLogError(
						"Error opening repository!", e);
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
	private Link logInLink;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String baseUrl;
	
	public AuthenticationDialog(
			String caption, RepositoryReference repositoryReference, 
			RepositoryListView repositoryListView, String baseUrl) {
		this.caption = caption;
		this.repositoryReference = repositoryReference;
		this.repositoryListView = repositoryListView;
		this.baseUrl = baseUrl;
		initComponents();
	}


	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		dialogWindow = new Window(caption, content);
		dialogWindow.setModal(true);
		
		Label termsOfUse = new Label(
				"By logging in you accept the " +
				"<a target=\"blank\" href=\"http://www.catma.de/termsofuse\">terms of use</a>!");
		termsOfUse.setContentMode(ContentMode.HTML);
		termsOfUse.setSizeFull();
		addComponent(termsOfUse);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		content.addComponent(this);
		
	}
	
	@Override
	public void attach() {
		super.attach();
		if (logInLink == null) {
			logInLink = createLogInLink(UI.getCurrent());
			if (logInLink == null) { //authorization code request failure
				addComponent(new Label("Unable to establish authentication!"));
				logger.log(Level.SEVERE, "Log-In-Link creation failed!");
			}
			else {
				addComponent(logInLink, 0);
			}
		}
	}

	private Link createLogInLink(UI ui) {
		try {
			String returnURL = baseUrl;
			String token = new BigInteger(130, new SecureRandom()).toString(32);

			// state token generation
			Totp totp = new Totp(
					RepositoryPropertyKey.otpSecret.getValue()+token, 
					new Clock(Integer.valueOf(RepositoryPropertyKey.otpDuration.getValue())));

			// creating the authorization request link 
			StringBuilder authenticationUrlBuilder = new StringBuilder();
			authenticationUrlBuilder.append(
				RepositoryPropertyKey.oauthAuthorizationCodeRequestURL.getValue());
			authenticationUrlBuilder.append("?client_id=");
			authenticationUrlBuilder.append(
				RepositoryPropertyKey.oauthClientId.getValue());
			authenticationUrlBuilder.append("&response_type=code");
			authenticationUrlBuilder.append("&scope=openid%20email&");
			authenticationUrlBuilder.append("&redirect_uri="+returnURL);
			authenticationUrlBuilder.append("&state=" + totp.now());
			authenticationUrlBuilder.append("&openid.realm="+returnURL);
			
			ClassResource icon =
					new ClassResource(
							"repository/resources/google.png");
			Link logInLink = 
					new Link(
						"Log in via Google", 
						new ExternalResource(authenticationUrlBuilder.toString()));
			logInLink.setIcon(icon);
			
			final AuthenticationRequestHandler authenticationRequestHandler =
					new AuthenticationRequestHandler(
							ui,
							returnURL, 
							repositoryReference,
							repositoryListView, 
							dialogWindow,
							token);
			
			
			VaadinSession.getCurrent().addRequestHandler(authenticationRequestHandler);
			
			btCancel.addClickListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					VaadinSession.getCurrent().removeRequestHandler(authenticationRequestHandler);
					
					UI.getCurrent().removeWindow(dialogWindow);
					
					Notification.show(
	                        "Authentication failure",
	                        "The authentication failed, you are not " +
	                        "allowed to access this repository!",
	                        Type.ERROR_MESSAGE);

				}
			});
			
			return logInLink;
		}
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error during authentication!", e);
		}
		return null;
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		UI.getCurrent().addWindow(dialogWindow);
	}
	
	public void show() {
		show("35%");
	}
	
}
