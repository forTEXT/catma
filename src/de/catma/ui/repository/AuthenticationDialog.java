package de.catma.ui.repository;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

import com.vaadin.Application;
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
	
	private static class AuthenticationParamHandler implements ParameterHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private Application application;
		private String returnURL;
		private ConsumerManager consumerManager;
		private DiscoveryInformation discovered;
		private URIHandler uriHandler;
		private RepositoryReference repositoryReference;
		private RepositoryManager repositoryManager;
		private Window dialogWindow;

		public AuthenticationParamHandler(Application application,
				String returnURL, ConsumerManager consumerManager,
				DiscoveryInformation discovered, 
				RepositoryReference repositoryReference,
				RepositoryManager repositoryManager, Window dialogWindow) {
			super();
			this.application = application;
			this.returnURL = returnURL;
			this.consumerManager = consumerManager;
			this.discovered = discovered;
			this.repositoryReference = repositoryReference;
			this.repositoryManager = repositoryManager;
			this.dialogWindow = dialogWindow;
		}

		public void handleParameters(final Map<String, String[]> parameters) {
			uriHandler = new URIHandler() {
				public DownloadStream handleURI(URL context, String relativeUri) {
					try {
						application.getMainWindow().removeURIHandler(this);
						application.getMainWindow().removeWindow(dialogWindow);
						dialogWindow = null;
						
						// extract the parameters from the authentication response
						// (which comes in as a HTTP request from the OpenID provider)
						ParameterList openidResp = new ParameterList(parameters);
						
						// verify the response
						VerificationResult verification = consumerManager.verify(
								returnURL, openidResp, discovered);
						
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
			                    
			                    Repository repository = 
		                    		repositoryManager.openRepository(
	                    				repositoryReference, userIdentification );
			                    
			                    ((CatmaApplication)application).openRepository(repository);
			                    
			                    return new DownloadStream(
			                    		application.getURL().openStream(), 
			                    		"text/html", "CLEA logged in");
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
						e.printStackTrace();
					}
					return null;	
				}
			};
			application.getMainWindow().addURIHandler(uriHandler);
		}
		
		public URIHandler getUriHandler() {
			return uriHandler;
		}
	}
	
	private Window dialogWindow;
	private String providerIdent;
	private String caption;
	private Button btCancel;
	private RepositoryReference repositoryReference;
	private RepositoryManager repositoryManager;
	
	public AuthenticationDialog(Application application,
			String caption, RepositoryReference repositoryReference, RepositoryManager repositoryManager) {
		this.caption = caption;
		this.repositoryReference = repositoryReference;
		this.repositoryManager = repositoryManager;
		this.providerIdent = "https://www.google.com/accounts/o8/id";
		initComponents(application);
	}


	private void initComponents(Application application) {
		setSizeFull();
		setSpacing(true);
		
		dialogWindow = new Window(caption);
		dialogWindow.setModal(true);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);

		Link logInLink = createLogInLink(application);
		if (logInLink != null) {
			addComponent(logInLink);
		}
		
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		dialogWindow.addComponent(this);
		
	}

	private Link createLogInLink(final Application application) {
		try {
			ConsumerManager consumerManager = new ConsumerManager();
			
			String returnURL = 
				application.getURL().toString() +
				new IDGenerator().generate();
			
			@SuppressWarnings("rawtypes")
			List discoveries = consumerManager.discover(this.providerIdent);
			final DiscoveryInformation discovered = 
					consumerManager.associate(discoveries);
			
			AuthRequest authReq = consumerManager.authenticate(discovered, returnURL);		
            FetchRequest fetch = FetchRequest.createFetchRequest();
            fetch.addAttribute("email",
                    // attribute alias
                    "http://schema.openid.net/contact/email",   // type URI
                    true);                                      // required

            // attach the extension to the authentication request
            authReq.addExtension(fetch);

			
			Link logInLink = 
					new Link(
						"Log in", 
						new ExternalResource(authReq.getDestinationUrl(true)));

			final AuthenticationParamHandler authenticationParamHandler =
					new AuthenticationParamHandler(
							application, returnURL, 
							consumerManager, discovered, 
							repositoryReference,
							repositoryManager, 
							dialogWindow);
					
			application.getMainWindow().addParameterHandler(
					authenticationParamHandler);
			btCancel.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					application.getMainWindow().removeURIHandler(
							authenticationParamHandler.getUriHandler());
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
