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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.UI;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.RepositoryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.Parameter;
import de.catma.ui.ParameterComponentValue;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.modules.main.login.AuthenticationDialog;
import de.catma.ui.tabbedview.TabComponent;
import de.catma.user.UserProperty;
import de.catma.util.IDGenerator;

public class RepositoryListView extends VerticalLayout implements TabComponent {

	private final static Logger LOGGER = Logger.getLogger(RepositoryListView.class.getName());
	private final static Cache<String,Integer> GUEST_ACCESS_COUNT_BY_IP = 
		CacheBuilder
			.newBuilder()
			.expireAfterWrite(
				RepositoryPropertyKey.GuestAccessCountExpirationInDays.getValue(1), 
				TimeUnit.DAYS)
			.concurrencyLevel(
				RepositoryPropertyKey.GuestAccessCountConcurrencyLevel.getValue(20))
			.build();
	
	private RepositoryManager repositoryManager;
	private Table repositoryTable;
	private Button openBt;
	
	public RepositoryListView(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
		initComponents();
		initActions();
	}
	
	private void initActions() {
		openBt.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				RepositoryReference repositoryReference = 
						(RepositoryReference)repositoryTable.getValue();
				if (repositoryManager.isOpen(repositoryReference)) {
					Notification.show(
							Messages.getString("RepositoryListView.infoTitle"), Messages.getString("RepositoryListView.repoAlreadyOpen"), //$NON-NLS-1$ //$NON-NLS-2$
							Type.TRAY_NOTIFICATION);
				}
				else {
					try {
						if (((CatmaApplication)UI.getCurrent()).getParameter(
										Parameter.USER_SPAWN_ASGUEST, "0").equals("1")) { //$NON-NLS-1$ //$NON-NLS-2$
							
							SpamProtectionDialog dlg = new SpamProtectionDialog(new SaveCancelListener<Void>() {
								@Override
								public void cancelPressed() {/*noop*/}
								@Override
								public void savePressed(Void result) {
									try {
										openAsGuest(repositoryReference);
									} catch (Exception e) {
										((CatmaApplication)UI.getCurrent()).showAndLogError(
												Messages.getString("RepositoryListView.errorOpeningRepo"), e); //$NON-NLS-1$
									}
								}
							});
							dlg.show();
						}
						else if (repositoryReference.isAuthenticationRequired()) {
							openWithAuthentication(repositoryReference);
						}
						else if ( ! repositoryReference.isAuthenticationRequired()) {
							openWithoutAuthentication(repositoryReference);
						}
					} catch (Exception e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("RepositoryListView.errorOpeningRepo"), e); //$NON-NLS-1$
					}
				}
			}
		});
		
		repositoryTable.addValueChangeListener(new Table.ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
            	openBt.setEnabled(event.getProperty().getValue() != null);
            }
		});
	}


	private void openWithoutAuthentication(RepositoryReference repositoryReference) throws Exception {
		String user = 
				((CatmaApplication)UI.getCurrent()).getParameter(
						Parameter.USER_IDENTIFIER);
		if (user == null) {
			user = System.getProperty("user.name"); //$NON-NLS-1$
		}
		Map<String,String> userIdentification = 
				new HashMap<String, String>(1);
		userIdentification.put(
			UserProperty.identifier.name(), user);
		open((CatmaApplication) getUI(),repositoryReference, userIdentification);
		Page.getCurrent().setLocation(RepositoryPropertyKey.BaseURL.getValue());
	}

	private void openAsGuest(RepositoryReference repositoryReference) throws Exception {
		WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
		String clientIpAddress = webBrowser.getAddress();
		if (clientIpAddress != null ) {
			Integer currentCount = 
					GUEST_ACCESS_COUNT_BY_IP.get(clientIpAddress, ()->0);
			
			if (currentCount > RepositoryPropertyKey.GuestAccessCountMax.getValue(50)) {
				GUEST_ACCESS_COUNT_BY_IP.put(clientIpAddress, currentCount+1);
			}
			else {
				LOGGER.warning("IP " + clientIpAddress + " has exceeded max guest access count!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			LOGGER.warning("could not check guest account access count, client IP is null!"); //$NON-NLS-1$
		}
		
		IDGenerator idGenerator = new IDGenerator();
		String userName = idGenerator.generate();
		Map<String,String> userIdentification = 
				new HashMap<String, String>(1);
		userIdentification.put(
				UserProperty.identifier.name(), userName);
		userIdentification.put(UserProperty.guest.name(), Boolean.TRUE.toString());
		
		open((CatmaApplication) getUI(), repositoryReference, userIdentification);
		
		Page.getCurrent().setLocation(RepositoryPropertyKey.BaseURL.getValue());

	}

	private void openWithAuthentication(RepositoryReference repositoryReference) {
		AuthenticationDialog authDialog = createAuthenticationDialog();
		authDialog.show();
	}
	
	public void open(
			CatmaApplication catmaApplication, // needs to be passed in, as getUI() may not be initialized yet 
			RepositoryReference repositoryReference, 
			Map<String,String> userIdentification) throws Exception {
		
//		catmaApplication.setUser(userIdentification);
		
		Repository repository = 
				repositoryManager.openRepository(
						repositoryReference, userIdentification);
		
		if (catmaApplication.getParameter(Parameter.USER_SPAWN, "0").equals("1")) { //$NON-NLS-1$ //$NON-NLS-2$
			repository.spawnContentFrom(
				catmaApplication.getParameter(Parameter.USER_IDENTIFIER), 
				catmaApplication.getParameter(Parameter.CORPORA_COPY, "0").equals("1"), //$NON-NLS-1$ //$NON-NLS-2$
				catmaApplication.getParameter(Parameter.TAGLIBS_COPY, "0").equals("1")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
//		catmaApplication.openRepository(repository);
		
		if (catmaApplication.getParameter(Parameter.COMPONENT) != null) {
			ParameterComponentValue.show(
					catmaApplication, catmaApplication.getParameter(Parameter.COMPONENT));
		}


	}

	public void open(
			CatmaApplication catmaApplication, // needs to be passed in, as getUI() may not be initialized yet 
			Map<String,String> userIdentification) throws Exception {

		if ((repositoryTable.getValue() == null) 
				&& !repositoryTable.getItemIds().isEmpty()) {
			repositoryTable.setValue(repositoryTable.getItemIds().iterator().next());
		}
		
		open(catmaApplication, (RepositoryReference) repositoryTable.getValue(), userIdentification);
	}
	
	private void initComponents() {
		repositoryTable = new Table(Messages.getString("RepositoryListView.availableRepos")); //$NON-NLS-1$
		BeanItemContainer<RepositoryReference> container = 
				new BeanItemContainer<RepositoryReference>(RepositoryReference.class);
		
		for (RepositoryReference ref : repositoryManager.getRepositoryReferences()) {
			container.addBean(ref);
		}
		
		repositoryTable.setContainerDataSource(container);

		repositoryTable.setVisibleColumns(new Object[] {"name"}); //$NON-NLS-1$
		repositoryTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		repositoryTable.setSelectable(true);
		repositoryTable.setMultiSelect(false);
		repositoryTable.setPageLength(3);
		repositoryTable.setImmediate(true);
		
		addComponent(repositoryTable);
		setMargin(true);
		setSpacing(true);
		
		
		openBt = new Button(Messages.getString("RepositoryListView.Open")); //$NON-NLS-1$
		
		
		addComponent(openBt);
		setComponentAlignment(openBt, Alignment.TOP_RIGHT);
		
		if (container.size() > 0) {
			repositoryTable.setValue(container.getIdByIndex(0));
		}
		else {
			openBt.setEnabled(false);
		}
	}
	
	public void openFirstRepository() {
		if ((repositoryTable.getValue() == null) 
				&& !repositoryTable.getItemIds().isEmpty()) {
			repositoryTable.setValue(repositoryTable.getItemIds().iterator().next());
		}
		
		if (repositoryTable.getValue() != null) {
			openBt.click();
		}
		else {
			Notification.show(
					Messages.getString("RepositoryListView.infoTitle"),  //$NON-NLS-1$
					Messages.getString("RepositoryListView.noAvailableRepos"), //$NON-NLS-1$
					Type.TRAY_NOTIFICATION);
		}
	}
	
	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

	public AuthenticationDialog createAuthenticationDialog() {
		RepositoryReference repositoryReference = 
				(RepositoryReference)repositoryTable.getValue();
		return createAuthenticationDialog(repositoryReference);
	}
	
	//TODO: should be removed!!
	//FIXME: please remove this !
	@Deprecated
	private AuthenticationDialog createAuthenticationDialog(RepositoryReference repositoryReference) {
		String baseURL = 
				RepositoryPropertyKey.BaseURL.getValue(
					RepositoryPropertyKey.BaseURL.getDefaultValue());
		return null; 
		
//		return new AuthenticationDialog(
//						Messages.getString("RepositoryListView.authenticateYourself"),  //$NON-NLS-1$
//						repositoryReference, 
//						this,
//						baseURL);
	}

}
