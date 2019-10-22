package de.catma.ui.events;

/**
 * This event indicates that resources e.g. {@link de.catma.document.source.SourceDocument} are dirty
 * and should be reloaded from Repository
 *
 * @author db
 */
public class ProjectChangedEvent {
	
	private String deletedProjectId;

	public ProjectChangedEvent() {
		this(null);
	}

	public ProjectChangedEvent(String projectId) {
		this.deletedProjectId = projectId;
	}

	public String getDeletedProjectId() {
		return deletedProjectId;
	}
}
