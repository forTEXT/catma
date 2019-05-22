/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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


package de.catma.backgroundservice;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executes background tasks. An  {@link ExecutionListener} is notified when 
 * the task is done. The notification takes place in the initiating Thread.<br>
 * <br>
 *
 * @author Marco Petris
 *
 */
public interface BackgroundService {
		
	/**
	 * @param <T> the type of the {@link ExecutionListener} and 
	 * 		the {@link ProgressCallable}.
	 *  
	 * @param callable the task which should be done in the background 
	 * @param listener this one will be notified when the task is 
	 * 		done (within the initiating thread)
	 * @param progressListener the listener for progress. The implementer 
	 *		of the {@link ProgressCallable} which defines the task can
	 *		call this listener to notify progress to the initiating thread.
	 */
	public <T> void submit( 
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener,
			final ProgressListener progressListener);	
	
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit);

	public ScheduledFuture<?> schedule(Runnable command,
			long delay, TimeUnit unit);
	public void shutdown();
}
