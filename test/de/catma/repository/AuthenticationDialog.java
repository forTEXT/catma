package de.catma.repository;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openid4java.association.Association;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
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

public class AuthenticationDialog extends VerticalLayout {
	
	private static class AuthenticationParamHandler implements ParameterHandler, URIHandler {
		
		private Logger logger = Logger.getLogger(this.getClass().getName());
		private Application application;
		private String returnURL;
		private ConsumerManager consumerManager;
		private DiscoveryInformation discovered;
		private Window dialogWindow;
		private ParameterList openidResp;

		public AuthenticationParamHandler(Application application,
				String returnURL, ConsumerManager consumerManager,
				DiscoveryInformation discovered, Window dialogWindow) {
			
			this.application = application;
			this.returnURL = returnURL;
			
			this.consumerManager = consumerManager;
			this.discovered = discovered;
			this.dialogWindow = dialogWindow;
		}

		public void handleParameters(Map<String, String[]> parameters) {
			openidResp = new ParameterList(parameters);
		}
					
		public DownloadStream handleURI(URL context, String relativeUri) {
			try {
				application.getMainWindow().removeURIHandler(this);
				
				application.getMainWindow().removeParameterHandler(this);
				
				application.getMainWindow().removeWindow(dialogWindow);
				
				dialogWindow = null;
				
				// verify the response
				VerificationResult verification = 
					consumerManager.verify(
						returnURL, openidResp, discovered);
				
				if (verification == null) {
					throw new MessageException(
							"could not verify return url: " + returnURL);
				}
				// examine the verification result and extract the verified identifier
				Identifier verified = verification.getVerifiedId();
				
				if (verified != null) {
					Map<String, String> userIdentification = 
							new HashMap<String, String>();

					AuthSuccess authSuccess =
	                        (AuthSuccess) verification.getAuthResponse();

	                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
	                    FetchResponse fetchResp = (FetchResponse) authSuccess
	                            .getExtension(AxMessage.OPENID_NS_AX);

	                    @SuppressWarnings("rawtypes")
						List emails = 
							fetchResp.getAttributeValues("email");
	                    
	                    String email = (String) emails.get(0);

	                    userIdentification.put(
								"user.ident", email);

	                    logger.info(
	                    		"user " 
	                    		+ userIdentification.get("user.ident") 
	                    		+ " logged in!");
	                    
	                    return new DownloadStream(
	                    		application.getURL().openStream(), 
	                    		"text/html", "");
	                }
				}
				else {
					logger.info("authentication failure");
					
					application.getMainWindow().showNotification(
                            "Authentication failure",
                            "The authentication failed!",
                            Notification.TYPE_ERROR_MESSAGE);

				}
				
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "authentication error", e);
			}
			
			application.close();
			
			return null;
		}
	}
	
	private Window dialogWindow;
	private String providerIdent;
	private String caption;
	private Button btCancel;
	private Link logInLink;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public AuthenticationDialog(String caption) {
		this.caption = caption;
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
				application.getURL().toString() 
				+ "ID_" + UUID.randomUUID().toString().toUpperCase();
			
			List<?> discoveries = consumerManager.discover(this.providerIdent);
			
			final DiscoveryInformation discovered = 
					consumerManager.associate(discoveries);

			Association assoc = 
					consumerManager.getAssociations().load(discovered.getOPEndpoint().toString());

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
							dialogWindow);
					
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
			logger.log(Level.SEVERE, "error creating login link", e);
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
