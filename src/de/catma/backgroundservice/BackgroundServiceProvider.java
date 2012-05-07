package de.catma.backgroundservice;

public interface BackgroundServiceProvider {
	public BackgroundService getBackgroundService();
	
	public <T> void submit( 
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener);
}
