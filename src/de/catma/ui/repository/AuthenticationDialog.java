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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openid4java.association.Association;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

import com.vaadin.server.ClassResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
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

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryReference;
import de.catma.ui.CatmaApplication;
import de.catma.util.IDGenerator;

public class AuthenticationDialog extends VerticalLayout {
	
	private static class AuthenticationRequestHandler implements RequestHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private String returnURL;
		private ConsumerManager consumerManager;
		private DiscoveryInformation discovered;
		private RepositoryReference repositoryReference;
		private RepositoryManager repositoryManager;
		private Window dialogWindow;
		private String handle;
		private ParameterList openidResp;
		private String signed;
		private String sig;
		private UI ui;

		public AuthenticationRequestHandler(
				UI ui, // UI.getCurrent() is not available during request handling, therefore we pass in the UI
				String returnURL, ConsumerManager consumerManager,
				DiscoveryInformation discovered, 
				RepositoryReference repositoryReference,
				RepositoryManager repositoryManager, Window dialogWindow, String handle) {
			super();
			this.ui = ui;
			this.returnURL = returnURL;
			logger.info("authentication dialog construction returnUrl: " + returnURL);
			this.consumerManager = consumerManager;
			this.discovered = discovered;
			this.repositoryReference = repositoryReference;
			this.repositoryManager = repositoryManager;
			this.dialogWindow = dialogWindow;
			this.handle = handle;
		}
			
		@Override
		public boolean handleRequest(VaadinSession session,
				VaadinRequest request, VaadinResponse response)
				throws IOException {

			try {
				openidResp = new ParameterList(request.getParameterMap());
				
				signed = openidResp.getParameterValue("openid.signed");
				sig = openidResp.getParameterValue("openid.sig");
				
				// extract the parameters from the authentication response
				// (which comes in as a HTTP request from the OpenID provider)
				if (!openidResp.hasParameter("openid.mode")) {
					logger.info("openid.mode is missing, trying to set required param to 'id_res'" );
					openidResp.set(new Parameter("openid.mode", "id_res"));
				}
				else {
					logger.info("got openid.mode: " + openidResp.getParameterValue("openid.mode"));
				}

				if (!openidResp.hasParameter("openid.return_to")) {
					logger.info("openid.return_to is missing, trying to set required param to " + returnURL);
					openidResp.set(new Parameter("openid.return_to", returnURL));
				}
				else {
					logger.info("got openid.return_to: " + openidResp.getParameterValue("openid.return_to"));
				}
				
				if (!openidResp.hasParameter("openid.assoc_handle")) {
					logger.info("openid.assoc_handle is missing, trying to set required param to " + handle);
					openidResp.set(new Parameter("openid.assoc_handle", handle));
				}
				else {
					logger.info("got openid.assoc_handle: " + openidResp.getParameterValue("openid.assoc_handle"));
				}
				
				if (!openidResp.hasParameter("openid.signed")) {
					logger.info("openid.signed is missing, setting to " + signed);
					openidResp.set(new Parameter("openid.signed", signed));
				}
				else {
					logger.info("got openid.signed: " + openidResp.getParameterValue("openid.signed"));
				}
				
				if (!openidResp.hasParameter("openid.sig")) {
					logger.info("openid.sig is missing, setting to " + sig);
					openidResp.set(new Parameter("openid.sig", sig));

				}
				else {
					logger.info("got openid.sig: " + openidResp.getParameterValue("openid.sig"));
				}
				
				VaadinSession.getCurrent().removeRequestHandler(this);
				
				if (ui == null) {
					throw new NullPointerException("UI not available!");
				}
				
				ui.removeWindow(dialogWindow);
				
				dialogWindow = null;
				logger.info("verifying returnurl: " + returnURL);
				
				// verify the response
				VerificationResult verification = 
					consumerManager.verify(
						returnURL, openidResp, discovered);
				
				if (verification == null) {
					throw new MessageException(
							"could not verify returnurl: " + returnURL);
				}
				// examine the verification result and extract the verified identifier
				Identifier verified = verification.getVerifiedId();
				
				if (verified != null) {
					Map<String, String> userIdentification = 
							new HashMap<String, String>();
					logger.info("verified user " + verified.getIdentifier());
					AuthSuccess authSuccess =
	                        (AuthSuccess) verification.getAuthResponse();

	                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
	                    FetchResponse fetchResp = (FetchResponse) authSuccess
	                            .getExtension(AxMessage.OPENID_NS_AX);

	                    @SuppressWarnings("rawtypes")
						List emails = 
							fetchResp.getAttributeValues("email");
	                    
	                    String email = (String) emails.get(0);
	                    logger.info("retrieved email: " + email);

	                    userIdentification.put(
								"user.ident", email);
	                    userIdentification.put(
	                    		"user.email", email);
	                    userIdentification.put(
	                    		"user.name", email);
	                    userIdentification.put(
	                    		"user.role", "0");
	                    
	                    logger.info("opening repository for user: " + email);
	                    
	                    ((CatmaApplication)ui).setUser(
	                    		userIdentification);

	                    Repository repository = 
                    		repositoryManager.openRepository(
                				repositoryReference, userIdentification );
	                    
	                    ((CatmaApplication)ui).openRepository(repository);

	                    new DownloadStream(
	                    		ui.getPage().getLocation().toURL().openStream(), 
	                    		"text/html", "CATMA 4.2").writeResponse(request, response);
	                    return true;
	                }
				}
				else {
					logger.info("authentication failure");
					Notification.show(
                            "Authentication failure",
                            "The authentication failed, you are not " +
                            "allowed to access this repository!",
                            Type.ERROR_MESSAGE);

				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
				((CatmaApplication)ui).showAndLogError(
						"Error opening repository!", e);
			}
			
			ui.close();
			
			return true;
		}
	}
	
	private Window dialogWindow;
	private String providerIdent;
	private String caption;
	private Button btCancel;
	private RepositoryReference repositoryReference;
	private RepositoryManager repositoryManager;
	private Link logInLink;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public AuthenticationDialog(
			String caption, RepositoryReference repositoryReference, 
			RepositoryManager repositoryManager) {
		this.caption = caption;
		this.repositoryReference = repositoryReference;
		this.repositoryManager = repositoryManager;
		this.providerIdent = "https://www.google.com/accounts/o8/id";
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
			addComponent(logInLink, 0);
		}
	}

	private Link createLogInLink(UI ui) {
		try {
			ConsumerManager consumerManager = new ConsumerManager();
			
			String returnURL = 
				Page.getCurrent().getLocation().toString() +
				new IDGenerator().generate();
			logger.info("return url in login link creation " + returnURL);
			
			@SuppressWarnings("rawtypes")
			List discoveries = consumerManager.discover(this.providerIdent);
			final DiscoveryInformation discovered = 
					consumerManager.associate(discoveries);
			logger.info("endpoint from consumer manager: " + discovered.getOPEndpoint().toString());
			Association assoc = 
					consumerManager.getAssociations().load(discovered.getOPEndpoint().toString());
			final String handle = assoc.getHandle();
			logger.info("handle from consumer manager: " + handle);

			AuthRequest authReq = consumerManager.authenticate(discovered, returnURL);		
            FetchRequest fetch = FetchRequest.createFetchRequest();
            fetch.addAttribute("email",
                    // attribute alias
                    "http://schema.openid.net/contact/email",   // type URI
                    true);                                      // required

            // attach the extension to the authentication request
            authReq.addExtension(fetch);

			ClassResource icon =
					new ClassResource(
							"repository/resources/google.png");
			Link logInLink = 
					new Link(
						"Log in via Google", 
						new ExternalResource(authReq.getDestinationUrl(true)));
			logInLink.setIcon(icon);
			
			final AuthenticationRequestHandler authenticationRequestHandler =
					new AuthenticationRequestHandler(
							ui,
							returnURL, 
							consumerManager, discovered, 
							repositoryReference,
							repositoryManager, 
							dialogWindow, 
							handle);
			
			
			VaadinSession.getCurrent().addRequestHandler(authenticationRequestHandler);
			
			btCancel.addClickListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					VaadinSession.getCurrent().removeRequestHandler(authenticationRequestHandler);
					
					UI.getCurrent().removeWindow(dialogWindow);
				}
			});
			
			return logInLink;
		}
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError("Error during authentication!", e);
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
