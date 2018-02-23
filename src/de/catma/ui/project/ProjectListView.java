package de.catma.ui.project;

import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.ui.tabbedview.TabComponent;

public class ProjectListView extends VerticalLayout implements TabComponent {
	
	private ProjectManager projectManager;

	
	public ProjectListView(ProjectManager projectManager) {
		this.projectManager = projectManager;

		initComponents();
		initData();
	}
	
	

	private void initData() {
		// TODO Auto-generated method stub
		
		projectManager.getProjectReferences();
	}



	private void initComponents() {
		// TODO Auto-generated method stub
		
	}



	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }
	
	
}
