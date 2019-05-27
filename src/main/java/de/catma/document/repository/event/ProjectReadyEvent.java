package de.catma.document.repository.event;

import de.catma.document.repository.Repository;

public class ProjectReadyEvent {
	private Repository project;

	public ProjectReadyEvent(Repository project) {
		super();
		this.project = project;
	}
	
	public Repository getProject() {
		return project;
	}
}
