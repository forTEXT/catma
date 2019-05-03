package de.catma.ui.events.routing;

import de.catma.project.ProjectReference;

/**
 * This event indicates that a project has been selected e.g. on the dashboard.
 * @author db
 *
 */
public class RouteToProjectEvent {

	private final ProjectReference projectReference;
	private boolean reloadProject;
	
	public RouteToProjectEvent(ProjectReference projectReference, boolean reloadProject) {
		this.projectReference = projectReference;
		this.reloadProject = reloadProject;
	}
	
	public RouteToProjectEvent() {
		this(null, false);
	}

	public ProjectReference getProjectReference() {
		return projectReference;
	}
	
	public boolean isReloadProject() {
		return reloadProject;
	}
}
