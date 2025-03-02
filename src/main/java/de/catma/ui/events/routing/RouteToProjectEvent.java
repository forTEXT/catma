package de.catma.ui.events.routing;

import de.catma.project.ProjectReference;

/**
 * This event indicates that a project has been selected, e.g. on the dashboard
 */
public class RouteToProjectEvent {
	private final ProjectReference projectReference;
	private final boolean needsForkConfiguration;

	public RouteToProjectEvent() {
		this(null, false);
	}

	public RouteToProjectEvent(ProjectReference projectReference) {
		this(projectReference, false);
	}

	public RouteToProjectEvent(ProjectReference projectReference, boolean needsForkConfiguration) {
		this.projectReference = projectReference;
		this.needsForkConfiguration = needsForkConfiguration;
	}

	public ProjectReference getProjectReference() {
		return projectReference;
	}
	
	public boolean isNeedsForkConfiguration() {
		return needsForkConfiguration;
	}
}
