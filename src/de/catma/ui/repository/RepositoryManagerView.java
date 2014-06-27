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

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.ui.tabbedview.TabbedView;

public class RepositoryManagerView extends TabbedView implements CloseHandler {

	private RepositoryListView repositoryListView;
	
	
	public RepositoryManagerView(RepositoryManager repositoryManager) {
		super("No repositories available.");
		repositoryListView = new RepositoryListView(repositoryManager);
		addTab(repositoryListView, "Repositories Overview");
	}


	public void openRepository(Repository repository) {
		RepositoryView repositoryView = getRepositoryView(repository);
		if (repositoryView != null) {
			setSelectedTab(repositoryView);
		}
		else {
			RepositoryView repoView = new RepositoryView(repository);
			addClosableTab(repoView, repository.getName());
			setSelectedTab(repoView);
		}
	}
	
	private RepositoryView getRepositoryView(Repository repository) {
		for (Component tabContent : this.getTabIterator()) {
			if (tabContent != repositoryListView) {
				RepositoryView view = (RepositoryView)tabContent;
				Repository curRepo = view.getRepository();
				if ((curRepo !=null) && curRepo.equals(repository)) {
					return view;
				}
			}
		}
		return null;
	}


	public RepositoryManager getRepositoryManager() {
		return repositoryListView.getRepositoryManager();
	}
	
	@Override
	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		RepositoryView view = (RepositoryView)tabContent;
		repositoryListView.getRepositoryManager().close(view.getRepository());
		super.onTabClose(tabsheet, tabContent);
	}


	public void openFirstRepository() {
		repositoryListView.openFirstRepository();
	}


	public void closeCurrentRepository() {
		closeClosables();
	}
}
