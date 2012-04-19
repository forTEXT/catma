package de.catma.ui.repository;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ParameterList;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.terminal.URIHandler;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import de.catma.CleaApplication;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.util.IDGenerator;

public class RepositoryListView extends VerticalLayout {

	private RepositoryManager repositoryManager;
	private Table repositoryTable;
	private Button openBt;
	
	public RepositoryListView(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
		initComponents();
		initActions();
	}

	private void initActions() {
		openBt.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
//				login();
				
				Repository repository = (Repository)repositoryTable.getValue();
				try {
					repository.open();
					((CleaApplication)getApplication()).openRepository(repository);
				} catch (Exception e) {
					ExceptionHandler.log(e);
				}
			}
		});
		
		repositoryTable.addListener(new Table.ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
            	openBt.setEnabled(event.getProperty().getValue() != null);
            }
		});
	}

	private void login() {
//		final String openIDProperty = "name";
//		PropertyCollection propertyCollection = 
//				new PropertyCollection(openIDProperty);
//
//		FormDialog openIDFormDialog =
//			new FormDialog(
//				"Enter your OpenID",
//				propertyCollection,
//				new FormDialog.SaveCancelListener() {
//					public void cancelPressed() {}
//					public void savePressed(
//							PropertysetItem propertysetItem) {
//						Property property = 
//								propertysetItem.getItemProperty(
//										openIDProperty);
//					}
//				});
//		
//		openIDFormDialog.getField(openIDProperty).setRequired(true);
//		openIDFormDialog.getField(openIDProperty).setRequiredError(
//						"You have to enter a name!");
//		openIDFormDialog.show(getApplication().getMainWindow());
//		

		authenticateWithOpenID();
		
	}

	private void authenticateWithOpenID() {
		// obtain a AuthRequest message to be sent to the OpenID provider
		try {
			System.out.println("pos1");
			final ConsumerManager consumerManager = new ConsumerManager();
			System.out.println("pos2");
			final String returnURL = "http://87.106.12.254:8080/clea/" + new IDGenerator().generate();
			System.out.println("pos3");
		    // perform discovery on the user-supplied identifier
		    List discoveries = consumerManager.discover("https://www.google.com/accounts/o8/id");
		    System.out.println("pos4 " + discoveries.size());
		    // attempt to associate with the OpenID provider
		    // and retrieve one service endpoint for authentication
		    final DiscoveryInformation discovered = consumerManager.associate(discoveries);
		    System.out.println("pos5 "+ discovered);
			AuthRequest authReq = consumerManager.authenticate(discovered, returnURL);
			
			addComponent(new Link("Log in", new ExternalResource(authReq.getDestinationUrl(true))));
			addComponent(new Label("test"));
			System.out.println("pos6");
			getApplication().getMainWindow().addParameterHandler(new ParameterHandler() {
				
				public void handleParameters(final Map<String, String[]> parameters) {
					System.out.println("pos7");
					getApplication().getMainWindow().addURIHandler(new URIHandler() {
						
						public DownloadStream handleURI(URL context, String relativeUri) {
							System.out.println("pos8 " + context);
							try {
								getApplication().getMainWindow().removeURIHandler(this);
								System.out.println("pos9");
								// extract the parameters from the authentication response
								// (which comes in as a HTTP request from the OpenID provider)
								ParameterList openidResp = new ParameterList(parameters);
								System.out.println("pos10");
								// verify the response
								VerificationResult verification = consumerManager.verify(
										returnURL, openidResp, discovered);
								System.out.println("pos11");
								// examine the verification result and extract the verified identifier
								Identifier verified = verification.getVerifiedId();
								System.out.println("pos12");
								if (verified != null) {
									getApplication().setUser(verified.getIdentifier());
									System.out.println("success: " + verified.getIdentifier() + " " + verified.toString());
								}
								else {
									System.out.println("no success");
								}
								
								return new DownloadStream(
									new URL("http://87.106.12.254:8080/clea/").openStream(), 
									"text/html", "CLEA logged in");
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							return null;
						}
					});
					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	private void initComponents() {
		repositoryTable = new Table("Available Repositories");
		BeanItemContainer<Repository> container = 
				new BeanItemContainer<Repository>(Repository.class);
		
		for (Repository r : repositoryManager.getRepositories()) {
			container.addBean(r);
		}
		
		repositoryTable.setContainerDataSource(container);

		repositoryTable.setVisibleColumns(new Object[] {"name"});
		repositoryTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
		repositoryTable.setSelectable(true);
		repositoryTable.setMultiSelect(false);
		repositoryTable.setPageLength(3);
		repositoryTable.setImmediate(true);
		
		addComponent(repositoryTable);
		setMargin(true);
		setSpacing(true);
		
		
		openBt = new Button("Open");
		openBt.setImmediate(true);
		
		
		addComponent(openBt);
		setComponentAlignment(openBt, Alignment.TOP_RIGHT);
		
		if (container.size() > 0) {
			repositoryTable.setValue(container.getIdByIndex(0));
		}
		else {
			openBt.setEnabled(false);
		}
		
		

	}
	
}
