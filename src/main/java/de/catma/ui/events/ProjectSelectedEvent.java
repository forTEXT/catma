package de.catma.ui.events;

import de.catma.project.ProjectReference;

/**
 * This event indicates that a project has been selected e.g. on the dashboard.
 * @author db
 *
 */
public class ProjectSelectedEvent {

	private final ProjectReference projectReference;
	
	public ProjectSelectedEvent(ProjectReference projectReference) {
		this.projectReference = projectReference;
	}
	
	public ProjectReference getProjectReference() {
		return projectReference;
	}
}
