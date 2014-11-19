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
import java.util.Properties;

import javax.naming.InitialContext;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.RepositoryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.tabbedview.TabComponent;

public class RepositoryListView extends VerticalLayout implements TabComponent {

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
							"Information", "Repository is already open.",
							Type.TRAY_NOTIFICATION);
				}
				else {
					try {	
						if (repositoryReference.isAuthenticationRequired()) {
							Properties properties = 
									(Properties) new InitialContext().lookup(
											RepositoryPropertiesName.CATMAPROPERTIES.name());
							
							String baseURL = 
									properties.getProperty(
										RepositoryPropertyKey.BaseURL.name(), 
										"http://www.digitalhumanities.it/catma/");
	
							AuthenticationDialog authDialog = 
									new AuthenticationDialog(
											"Please authenticate yourself", 
											repositoryReference, repositoryManager,
											baseURL);
							authDialog.show();
						}
						else {
							String user = 
								((CatmaApplication)UI.getCurrent()).getParameter(
										"user.name");
							if (user == null) {
								user = System.getProperty("user.name");
							}
							Map<String,String> userIdentification = 
									new HashMap<String, String>(1);
							userIdentification.put(
								"user.ident", user);
							
							((CatmaApplication)UI.getCurrent()).setUser(userIdentification);
							
							Repository repository = 
									repositoryManager.openRepository(
											repositoryReference, userIdentification);
							
							((CatmaApplication)UI.getCurrent()).openRepository(
									repository);
						}
					} catch (Exception e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(
								"Error opening the repository!", e);
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


	private void initComponents() {
		repositoryTable = new Table("Available Repositories");
		BeanItemContainer<RepositoryReference> container = 
				new BeanItemContainer<RepositoryReference>(RepositoryReference.class);
		
		for (RepositoryReference ref : repositoryManager.getRepositoryReferences()) {
			container.addBean(ref);
		}
		
		repositoryTable.setContainerDataSource(container);

		repositoryTable.setVisibleColumns(new Object[] {"name"});
		repositoryTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
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
					"Information", 
					"There are no available repositories to login!",
					Type.TRAY_NOTIFICATION);
		}
	}
	
	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
