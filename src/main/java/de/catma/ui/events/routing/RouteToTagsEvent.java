package de.catma.ui.events.routing;

import de.catma.project.Project;


public class RouteToTagsEvent {

	private final Project project;
	
	public RouteToTagsEvent(Project project) {
		super();
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

}
