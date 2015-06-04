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
package de.catma.ui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.naming.NamingException;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.repository.RepositoryManagerView;

public class LoginLogoutCommand implements Command {
	private MenuItem loginLogoutItem;
	private RepositoryManagerView repositoryManagerView;
	private MainMenu menu;
	private String afterLogoutRedirectURL;
	
	private PropertyChangeListener repositoryManagerListener = 
			new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			if (repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
				loginLogoutItem.setText("Logout");
			}
			else {
				loginLogoutItem.setText("Login");
				logout();
			}
		}
	};
	
	public LoginLogoutCommand(
			MainMenu menu, RepositoryManagerView repositoryManagerView) throws NamingException {
		this.afterLogoutRedirectURL = 
				RepositoryPropertyKey.BaseURL.getValue( 
						RepositoryPropertyKey.BaseURL.getDefaultValue());
		
		this.menu = menu;
		this.repositoryManagerView = repositoryManagerView;
		repositoryManagerView.getRepositoryManager().
			addPropertyChangeListener(
				RepositoryManager.RepositoryManagerEvent.repositoryStateChange, 
				repositoryManagerListener);
	}


	public void menuSelected(MenuItem selectedItem) {
		if (repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
			repositoryManagerView.closeCurrentRepository();
			logout();
		}
		else {
			menu.executeEntry(repositoryManagerView);
			repositoryManagerView.openFirstRepository();
		}
	}
	
	private void logout() {
		Page.getCurrent().setLocation(afterLogoutRedirectURL);
		VaadinSession.getCurrent().close();
	}


	public void setLoginLogoutItem(MenuItem loginLogoutitem) {
		this.loginLogoutItem = loginLogoutitem;	
	}
}