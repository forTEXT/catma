package de.catma.project;

import de.catma.project.conflict.ConflictedProject;

public interface OpenProjectListener {
	public void progress(String msg, Object... params);
	public void ready(Project project);
	public void failure(Throwable t);
	public void conflictResolutionNeeded(ConflictedProject project);
}
