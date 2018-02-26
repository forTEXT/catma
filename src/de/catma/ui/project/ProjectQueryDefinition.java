package de.catma.ui.project;

import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;

import de.catma.project.ProjectManager;

public class ProjectQueryDefinition extends LazyQueryDefinition {
	
	private ProjectManager projectManager;

	public ProjectQueryDefinition(
			ProjectManager projectManager) {
		super(false, 30, "projectId");
		this.projectManager = projectManager;
		addProperty("name", String.class, null, true, false);
	}
	
	ProjectManager getProjectManager() {
		return projectManager;
	}
	

}
