package de.catma.ui.repository;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import de.catma.CleaApplication;
import de.catma.core.document.repository.Repository;
import de.catma.core.util.IDGenerator;

public class AuthenticationDialog extends VerticalLayout {
	
	private static class AuthenticationParamHandler implements ParameterHandler {
		
		private Application application;
		private String returnURL;
		private ConsumerManager consumerManager;
		private DiscoveryInformation discovered;
		private URIHandler uriHandler;
		private Repository repository;
		
		public AuthenticationParamHandler(Application application,
				String returnURL, ConsumerManager consumerManager,
				DiscoveryInformation discovered, Repository repository) {
			super();
			this.application = application;
			this.returnURL = returnURL;
			this.consumerManager = consumerManager;
			this.discovered = discovered;
			this.repository = repository;
		}

		public void handleParameters(final Map<String, String[]> parameters) {
			uriHandler = new URIHandler() {
				public DownloadStream handleURI(URL context, String relativeUri) {
					try {
						application.getMainWindow().removeURIHandler(this);
						
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
							userIdentification.put(
									"user.ident", verified.getIdentifier());
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
			                    		"user.email", email);
			                    userIdentification.put(
			                    		"user.name", email);
			                }

							repository.open(userIdentification);
							((CleaApplication)application).openRepository(repository);
						}
						else {
							System.out.println("no success");
						}
						
						return new DownloadStream(
							application.getURL().openStream(), 
							"text/html", "CLEA logged in");
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
	
	public interface CancelListener {
		public void cancelPressed();
	}
	
	private Window dialogWindow;
	private String providerIdent;
	private String caption;
	private Button btCancel;
	private Repository repository;
	
	public AuthenticationDialog(
			String caption, Repository repository) {
		this.caption = caption;
		this.repository = repository;
		this.providerIdent = "https://www.google.com/accounts/o8/id";
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

		Link logInLink = createLogInLink();
		if (logInLink != null) {
			addComponent(logInLink);
		}
		
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		dialogWindow.addComponent(this);
		
	}

	@Override
	public void attach() {
		super.attach();
		initComponents();
	}

	private Link createLogInLink() {
		try {
			ConsumerManager consumerManager = new ConsumerManager();
			
			String returnURL = 
				getApplication().getURL().toString() +
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
							getApplication(), returnURL, 
							consumerManager, discovered, repository);
					
			getApplication().getMainWindow().addParameterHandler(
					authenticationParamHandler);
			btCancel.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					getApplication().getMainWindow().removeURIHandler(
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
