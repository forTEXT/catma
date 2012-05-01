package de.catma.ui;

import com.vaadin.ui.ProgressIndicator;

import de.catma.backgroundservice.ProgressListener;

public class DefaultProgressListener implements ProgressListener {

	private ProgressIndicator pi;
	private Object lock;
	
	public DefaultProgressListener(ProgressIndicator pi, Object lock) {
		this.pi = pi;
		this.lock = lock;
	}
	
	public void setProgress(String value, Object... args) {
		synchronized (lock) {
			pi.setCaption(value);
		}		
	}
}
