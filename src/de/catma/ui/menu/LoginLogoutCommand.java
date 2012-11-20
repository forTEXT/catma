package de.catma.ui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.document.repository.RepositoryManager;
import de.catma.ui.repository.RepositoryManagerView;

public class LoginLogoutCommand implements Command {
	private boolean isLoggedIn = false;
	private MenuItem loginLogoutItem;
	private RepositoryManagerView repositoryManagerView;
	private Menu menu;
	
	private PropertyChangeListener repositoryManagerListener = 
			new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			isLoggedIn = repositoryManagerView.getRepositoryManager().hasOpenRepository();
			if (isLoggedIn) {
				loginLogoutItem.setText("Logout");
			}
			else {
				loginLogoutItem.setText("Login");
			}
		}
	};
	
	public LoginLogoutCommand(Menu menu, RepositoryManagerView repositoryManagerView) {
		this.menu = menu;
		this.repositoryManagerView = repositoryManagerView;
		repositoryManagerView.getRepositoryManager().
			addPropertyChangeListener(
				RepositoryManager.RepositoryManagerEvent.repositoryStateChange, 
				repositoryManagerListener);
	}


	public void menuSelected(MenuItem selectedItem) {
		if (isLoggedIn) {
			repositoryManagerView.closeCurrentRepository();
			if (!repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
				isLoggedIn = false;
				selectedItem.setText("Login");
			}
		}
		else {
			menu.executeEntry(repositoryManagerView);
			if (repositoryManagerView.openFirstRepository()) {
				isLoggedIn = true;
				selectedItem.setText("Logout");
			}
		}
	}


	public void setLoginLogoutItem(MenuItem loginLogoutitem) {
		this.loginLogoutItem = loginLogoutitem;	
	}
}