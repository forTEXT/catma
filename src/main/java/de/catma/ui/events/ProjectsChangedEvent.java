package de.catma.ui.events;

/**
 * This event is fired when a project is created, deleted, joined or left and
 * signals to {@link de.catma.ui.module.dashboard.ProjectListView} that the list of projects needs to be refreshed.
 */
public class ProjectsChangedEvent {
	private final String deletedProjectId;

	public ProjectsChangedEvent() {
		this(null);
	}

	public ProjectsChangedEvent(String deletedProjectId) {
		this.deletedProjectId = deletedProjectId;
	}

	public String getDeletedProjectId() {
		return deletedProjectId;
	}
}
