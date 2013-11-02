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

import java.net.URL;
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

import com.vaadin.Application;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.terminal.URIHandler;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryReference;
import de.catma.util.IDGenerator;

public class AuthenticationDialog extends VerticalLayout {
	
	private static class AuthenticationParamHandler implements ParameterHandler, URIHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private Application application;
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

		public AuthenticationParamHandler(Application application,
				String returnURL, ConsumerManager consumerManager,
				DiscoveryInformation discovered, 
				RepositoryReference repositoryReference,
				RepositoryManager repositoryManager, Window dialogWindow, String handle) {
			super();
			this.application = application;
			this.returnURL = returnURL;
			logger.info("authentication dialog construction returnUrl: " + returnURL);
			this.consumerManager = consumerManager;
			this.discovered = discovered;
			this.repositoryReference = repositoryReference;
			this.repositoryManager = repositoryManager;
			this.dialogWindow = dialogWindow;
			this.handle = handle;
		}

		public void handleParameters(Map<String, String[]> parameters) {
			openidResp = new ParameterList(parameters);
			
			signed = openidResp.getParameterValue("openid.signed");
			sig = openidResp.getParameterValue("openid.sig");
			
		}
					
		public DownloadStream handleURI(URL context, String relativeUri) {
			try {
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
				
				application.getMainWindow().removeURIHandler(this);
				
				application.getMainWindow().removeParameterHandler(this);
				
				application.getMainWindow().removeWindow(dialogWindow);
				
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
	                    
	                    application.setUser(userIdentification);

	                    Repository repository = 
                    		repositoryManager.openRepository(
                				repositoryReference, userIdentification );
	                    
	                    ((CatmaApplication)application).openRepository(repository);
	                    
	                    
	                    return new DownloadStream(
	                    		application.getURL().openStream(), 
	                    		"text/html", "CATMA 4");
	                }
				}
				else {
					logger.info("authentication failure");
					application.getMainWindow().showNotification(
                            "Authentication failure",
                            "The authentication failed, you are not " +
                            "allowed to access this repository!",
                            Notification.TYPE_ERROR_MESSAGE);

				}
				
			}
			catch (Exception e) {
				((CatmaApplication)application).showAndLogError(
						"Error opening repository!", e);
			}
			
			application.close();
			
			return null;
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
		
		dialogWindow = new Window(caption);
		dialogWindow.setModal(true);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		dialogWindow.addComponent(this);
		
	}
	
	@Override
	public void attach() {
		super.attach();
		if (logInLink == null) {
			logInLink = createLogInLink(getApplication());
			addComponent(logInLink, 0);
		}
	}

	private Link createLogInLink(final Application application) {
		try {
			ConsumerManager consumerManager = new ConsumerManager();
			
			String returnURL = 
				application.getURL().toString() +
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
							"ui/repository/resources/google.png", application);
			Link logInLink = 
					new Link(
						"Log in via Google", 
						new ExternalResource(authReq.getDestinationUrl(true)));
			logInLink.setIcon(icon);
			
			final AuthenticationParamHandler authenticationParamHandler =
					new AuthenticationParamHandler(
							application, returnURL, 
							consumerManager, discovered, 
							repositoryReference,
							repositoryManager, 
							dialogWindow, 
							handle);
					
			application.getMainWindow().addParameterHandler(
					authenticationParamHandler);
			application.getMainWindow().addURIHandler(
					authenticationParamHandler);
			
			btCancel.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					application.getMainWindow().removeParameterHandler(
							authenticationParamHandler);
					application.getMainWindow().removeURIHandler(
							authenticationParamHandler);
					application.getMainWindow().removeWindow(dialogWindow);
				}
			});
			
			return logInLink;
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO: handle
		}
		return null;
	}
	
	public void show(Window parent, String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		parent.addWindow(dialogWindow);
	}
	
	public void show(Window parent) {
		show(parent, "25%");
	}
	
}
