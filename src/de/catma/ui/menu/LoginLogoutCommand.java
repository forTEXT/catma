package de.catma.ui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.Application;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.document.repository.RepositoryManager;
import de.catma.ui.repository.RepositoryManagerView;

public class LoginLogoutCommand implements Command {
	private MenuItem loginLogoutItem;
	private RepositoryManagerView repositoryManagerView;
	private Menu menu;
	final private Application application;
	
	private PropertyChangeListener repositoryManagerListener = 
			new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			if (repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
				loginLogoutItem.setText("Logout");
			}
			else {
				loginLogoutItem.setText("Login");
				application.close();
			}
		}
	};
	
	public LoginLogoutCommand(
			Menu menu, RepositoryManagerView repositoryManagerView, 
			Application application) {
		this.menu = menu;
		this.repositoryManagerView = repositoryManagerView;
		this.application = application;
		repositoryManagerView.getRepositoryManager().
			addPropertyChangeListener(
				RepositoryManager.RepositoryManagerEvent.repositoryStateChange, 
				repositoryManagerListener);
	}


	public void menuSelected(MenuItem selectedItem) {
		if (repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
			repositoryManagerView.closeCurrentRepository();
			application.close();
		}
		else {
			menu.executeEntry(repositoryManagerView);
			repositoryManagerView.openFirstRepository();
		}
	}


	public void setLoginLogoutItem(MenuItem loginLogoutitem) {
		this.loginLogoutItem = loginLogoutitem;	
	}
}