package de.catma.ui.common.client.ui.event;

import java.util.ArrayList;
import java.util.List;

public class EventListenerSupport {

	private List<EventListener> eventListeners;
	
	public EventListenerSupport() {
		eventListeners = new ArrayList<EventListener>();
	}
	
	public void addEventListener(EventListener eventListener) {
		eventListeners.add(eventListener);
	}
	
	public void removeEventListener(EventListener eventListener) {
		eventListeners.remove(eventListener);
	}
	
	public void clear() {
		eventListeners.clear();
	}
}
