package de.catma.ui.events.routing;

import de.catma.project.ProjectReference;

/**
 * This event indicates that a project has been selected, e.g. on the dashboard
 */
public class RouteToProjectEvent {
	private final ProjectReference projectReference;

	public RouteToProjectEvent() {
		this(null);
	}

	public RouteToProjectEvent(ProjectReference projectReference) {
		this.projectReference = projectReference;
	}

	public ProjectReference getProjectReference() {
		return projectReference;
	}
}
