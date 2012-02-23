/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.client.ui.common.event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author marco.petris@web.de
 *
 */
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
	
	public void fireEvent(Object event) {
		for (EventListener listener : eventListeners) {
			listener.eventFired(event);
		}
	}
	
	public void clear() {
		eventListeners.clear();
	}
}
