package de.catma.project;

import de.catma.document.repository.Repository;
import de.catma.project.conflict.ConflictedProject;

public interface OpenProjectListener {
	public void progress(String msg, Object... params);
	public void ready(Repository project);
	public void failure(Throwable t);
	public void conflictResolutionNeeded(ConflictedProject project);
}
