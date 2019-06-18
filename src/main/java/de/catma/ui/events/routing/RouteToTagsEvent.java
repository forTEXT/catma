package de.catma.ui.events.routing;

import de.catma.document.repository.Repository;


public class RouteToTagsEvent {

	private final Repository project;
	
	public RouteToTagsEvent(Repository project) {
		super();
		this.project = project;
	}

	public Repository getProject() {
		return project;
	}

}
