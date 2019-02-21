package de.catma.ui.events;

import java.io.Closeable;

/**
 * Signals a closeable method should be invoked on session end.
 *  
 * @author db
 *
 */
public class CloseableEvent {

	private final Closeable closeable;

	public CloseableEvent(Closeable closeable){
		this.closeable = closeable;
	}
	
	public Closeable getCloseable() {
		return closeable;
	}
}
