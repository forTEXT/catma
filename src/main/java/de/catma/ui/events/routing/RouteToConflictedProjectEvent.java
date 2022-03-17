package de.catma.ui.events.routing;

import de.catma.project.conflict.ConflictedProject;

/**
 * This event indicates that a conflicted project nees conflict resolution.
 *
 */
@Deprecated
public class RouteToConflictedProjectEvent {

	private final ConflictedProject conflictedProject;
	
	public RouteToConflictedProjectEvent(ConflictedProject conflictedProject) {
		this.conflictedProject = conflictedProject;
	}
	
	 public ConflictedProject getConflictedProject() {
		return conflictedProject;
	}
}
