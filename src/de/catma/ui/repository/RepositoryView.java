package de.catma.ui.repository;

import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;


public class RepositoryView extends VerticalLayout {

	private Repository repository;
	
	public RepositoryView(Repository repository) {
		super();
		this.repository = repository;
		initComponents();
	}

	private void initComponents() {
		
		
	}

	public Repository getRepository() {
		return repository;
	}
}
