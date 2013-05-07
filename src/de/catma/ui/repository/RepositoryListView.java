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

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryReference;
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
		openBt.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				RepositoryReference repositoryReference = 
						(RepositoryReference)repositoryTable.getValue();
				if (repositoryManager.isOpen(repositoryReference)) {
					getWindow().showNotification(
							"Information", "Repository is already open.",
							Notification.TYPE_TRAY_NOTIFICATION);
				}
				else {
					if (repositoryReference.isAuthenticationRequired()) {
						AuthenticationDialog authDialog = 
								new AuthenticationDialog(
										"Please authenticate yourself", 
										repositoryReference, repositoryManager);
						authDialog.show(getApplication().getMainWindow());
					}
					else {
						try {
							String user = 
								((CatmaApplication)getApplication()).getParameter(
										"user.name");
							if (user == null) {
								user = System.getProperty("user.name");
							}
							Map<String,String> userIdentification = 
									new HashMap<String, String>(1);
							userIdentification.put(
								"user.ident", user);
							
							getApplication().setUser(userIdentification);
							
							Repository repository = 
									repositoryManager.openRepository(
											repositoryReference, userIdentification);
							
							((CatmaApplication)getApplication()).openRepository(
									repository);
							
						} catch (Exception e) {
							((CatmaApplication)getApplication()).showAndLogError(
								"Error opening the repository!", e);
						}
					}
				}
			}
		});
		
		repositoryTable.addListener(new Table.ValueChangeListener() {
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
	
	public void openFirstRepository() {
		if ((repositoryTable.getValue() == null) 
				&& !repositoryTable.getItemIds().isEmpty()) {
			repositoryTable.setValue(repositoryTable.getItemIds().iterator().next());
		}
		
		if (repositoryTable.getValue() != null) {
			openBt.click();
		}
		else {
			getApplication().getMainWindow().showNotification(
					"Information", 
					"There are no available repositories to login!",
					Notification.TYPE_TRAY_NOTIFICATION);
		}
	}
	
	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
