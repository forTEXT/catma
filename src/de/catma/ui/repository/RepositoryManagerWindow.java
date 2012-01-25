package de.catma.ui.repository;

import com.vaadin.ui.Window;


public class RepositoryManagerWindow extends Window {

	public RepositoryManagerWindow(RepositoryManagerView view) {
		super("Repository Manager");
		this.setContent(view);
		
		setWidth("50%");
		setHeight("90%");
	}
}
