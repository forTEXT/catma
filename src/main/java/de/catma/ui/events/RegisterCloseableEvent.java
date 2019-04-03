package de.catma.ui.events;

import java.io.Closeable;

/**
 * Signals a closeable method should be invoked on session end.
 *  
 * @author db
 *
 */
public class RegisterCloseableEvent {

	private final Closeable closeable;

	public RegisterCloseableEvent(Closeable closeable){
		this.closeable = closeable;
	}
	
	public Closeable getCloseable() {
		return closeable;
	}
}
