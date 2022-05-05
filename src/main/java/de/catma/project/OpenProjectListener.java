package de.catma.project;

public interface OpenProjectListener {
	public void progress(String msg, Object... params);
	public void ready(Project project);
	public void failure(Throwable t);
}
