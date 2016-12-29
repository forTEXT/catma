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
import java.util.Map;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;

import de.catma.document.repository.RepositoryManager;
import de.catma.ui.CatmaApplication;
import de.catma.ui.repository.RepositoryManagerView;
import de.catma.user.UserProperty;

public class LoginLogoutCommand implements Command {
	private Button btLoginLogout;
	private RepositoryManagerView repositoryManagerView;
	private MainMenu menu;
	private String afterLogoutRedirectURL;
	
	private PropertyChangeListener repositoryManagerListener = 
			new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			if (repositoryManagerView.getRepositoryManager().hasOpenRepository()) {
				btLoginLogout.setHtmlContentAllowed(true);
				
				@SuppressWarnings("unchecked")
				Map<String,String> userInfo =  (Map<String, String>) ((CatmaApplication)UI.getCurrent()).getUser();
				
				String identifier = userInfo.get(UserProperty.identifier.name()).toString();
				if (Boolean.valueOf(userInfo.get(UserProperty.guest.name()))) {
					identifier = "Guest";
				}
				
				btLoginLogout.setCaption(identifier + " - Sign out");
			}
			else {
				btLoginLogout.setCaption("Sign in");
				logout();
			}
		}
	};
	
	public LoginLogoutCommand(
		MainMenu menu, RepositoryManagerView repositoryManagerView) {
//		this.afterLogoutRedirectURL = 
//				RepositoryPropertyKey.BaseURL.getValue( 
//						RepositoryPropertyKey.BaseURL.getDefaultValue());
		
		String scheme = VaadinServletService.getCurrentServletRequest().getScheme();		
		String serverName = VaadinServletService.getCurrentServletRequest().getServerName();		
		Integer port = VaadinServletService.getCurrentServletRequest().getServerPort();
		String contextPath = VaadinService.getCurrentRequest().getContextPath();
		
		String baseUrl = String.format("%s://%s%s%s", scheme, serverName, port == 80 ? "" : ":"+port, contextPath);
		this.afterLogoutRedirectURL = baseUrl;
		
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


	public void setLoginLogoutButton(Button btLoginLogout) {
		this.btLoginLogout = btLoginLogout;	
	}
}