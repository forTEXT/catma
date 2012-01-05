package de.catma.ui.repository;

import java.util.Iterator;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;

public class RepositoryManagerView extends VerticalLayout implements CloseHandler {

	private TabSheet tabSheet;
	private RepositoryListView repositoryListView;
	
	
	public RepositoryManagerView(RepositoryManager repositoryManager) {
		tabSheet = new TabSheet();
		repositoryListView = new RepositoryListView(repositoryManager);
		Tab tab = tabSheet.addTab(repositoryListView, "Repositories Overview");
		
		tab.setEnabled(true);
		addComponent(tabSheet);
		tabSheet.setCloseHandler(this);
	}


	public void openRepository(Repository repository) {
		RepositoryView repositoryView = getRepositoryView(repository);
		if (repositoryView != null) {
			tabSheet.setSelectedTab(repositoryView);
		}
		else {
			Tab tab = tabSheet.addTab(new RepositoryView(repository), repository.getName());
			tab.setClosable(true);
			tabSheet.setSelectedTab(tab.getComponent());
		}
	}
	
	private RepositoryView getRepositoryView(Repository repository) {
		Iterator<Component> iterator = tabSheet.getComponentIterator();
		while (iterator.hasNext()) {
			Component c = iterator.next();
			if (c != repositoryListView) {
				RepositoryView view = (RepositoryView)c;
				Repository curRepo = view.getRepository();
				if ((curRepo !=null) && curRepo.equals(repository)) {
					return view;
				}
			}
		}
		return null;
	}
	
	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		// workaround for http://dev.vaadin.com/ticket/7686
		
//		if (tabContent.equals(tabsheet.getSelectedTab())) {
//			tabsheet.removeComponent(tabContent);
//		}
//		else {
//			tabsheet.setSelectedTab(tabContent);
//		}
		
		tabsheet.removeComponent(tabContent);
		try {
			Thread.sleep(5);
		} catch (InterruptedException ex) {
	            //do nothing 
	    }
	}
}
