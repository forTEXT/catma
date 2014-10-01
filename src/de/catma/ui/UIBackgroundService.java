package de.catma.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.backgroundservice.ProgressListener;

public class UIBackgroundService implements BackgroundService {

	private ExecutorService backgroundThread;
	private boolean background;
	
	public UIBackgroundService(boolean background) {
		this.background = background;
		if (background) {
			backgroundThread = Executors.newFixedThreadPool(2);
		}
	}


	@Override
	public <T> void submit(final ProgressCallable<T> callable,
			final ExecutionListener<T> listener, final ProgressListener progressListener) {
        if (background) {
            backgroundThread.submit( new Runnable() {
                public void run() {
                    try {
                        callable.setProgressListener( progressListener );
                        final T result = callable.call();
                        if (UI.getCurrent() != null && UI.getCurrent().isAttached()) {
	                        UI.getCurrent().access(new Runnable() {
	                        	public void run() {
	                        		listener.done(result);
	                        		UI.getCurrent().push();
	                        	}
	                        });
                        }                        
                    } catch (final Throwable t) {
                        try {
                        	Logger.getLogger(
                        			getClass().getName()).log(
                        					Level.SEVERE, "error", t);
                        	if (UI.getCurrent() != null && UI.getCurrent().isAttached()) {
	                            UI.getCurrent().access(new Runnable() {
	                            	public void run() {
	                            		listener.error(t);
	                            		UI.getCurrent().push();
	                            	}
	                            });
                        	}
                        }
                        catch(Throwable t2) {
                        	t2.printStackTrace();
                        }
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
            	listener.error(t);
            	Logger.getLogger(getClass().getName()).log(
            		Level.SEVERE, "error", t);  
            }
        }
	}

}
