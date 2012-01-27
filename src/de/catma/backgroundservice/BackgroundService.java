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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes background tasks. An  {@link ExecutionListener} is notified when 
 * the task is done. The notification takes place in the GUI-Thread.<br>
 * <br>
 *
 * @author Marco Petris
 *
 */
public class BackgroundService {
	
	private ExecutorService backgroundThread;
	private ProgressListener progressListener;
	private boolean background = true;
	private Object lock;

	/**
	 * setup the worker thread
	 */
	public BackgroundService(Object lock) {
		this.lock = lock;
		backgroundThread = Executors.newSingleThreadExecutor();
	}

	/**
	 * @param <T> the type of the {@link ExecutionListener} and 
	 * the {@link ProgressCallable}.
	 *  
	 * @param callable the task which should be done in the background 
	 * @param listener this one will be notified when the task is 
	 * done (within the GUI-Thread)
	 */
	public <T> void submit( 
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener ) {
        if (background) {
            backgroundThread.submit( new Runnable() {
                public void run() {
                    try {
                        callable.setProgressListener( progressListener );
                        final T result = callable.call();
                        
                        synchronized(lock) {
                        	listener.done(result);
                        }
                    } catch (Throwable t) {
                        try {
                            progressListener.setIndeterminate(false, "");
                        }
                        catch(Throwable ignored) {}
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error", t);
                    }
                }
            } );
        }
        else {
            try {
                callable.setProgressListener( progressListener );
                final T result = callable.call();
                listener.done( result );

            } catch (Throwable t) {
            	Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error", t);
            }
        }
		
	}
	
	/**
	 * Sets the {@link ProgressListener} which will be notified during task execution.
	 * This is done by the implementer of the {@link ProgressCallable} which defines the task.
	 * @param progressListener the listener for progress
	 */
	public void setProgressListener( ProgressListener progressListener ) {
		this.progressListener = progressListener;
	}

    /**
     * Shutdown the background service. This method has been added for testing purposes!
     */
    public void shutdown()  {
        backgroundThread.shutdown();
        background = false;
    }
	
}
