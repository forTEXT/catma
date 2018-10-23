package de.catma.v10ui.background;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.backgroundservice.ProgressListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UIBackgroundService implements BackgroundService {

	private ScheduledExecutorService backgroundThread;
	private boolean background;
	
	public UIBackgroundService(boolean background) {
		this.background = background;
		if (background) {
			backgroundThread = Executors.newScheduledThreadPool(2);
		}
	}


	@Override
	public <T> void submit(final ProgressCallable<T> callable,
			final ExecutionListener<T> listener, final ProgressListener progressListener) {
        if (background) {
        	final UI ui = UI.getCurrent();
            backgroundThread.submit( new Runnable() {
                public void run() {
                    try {
                        callable.setProgressListener( progressListener );
                        final T result = callable.call();
                        if (ui != null) {
	                        ui.access(new Command() {
	                        	public void execute() {
	                        		try {
	                        			listener.done(result);
	                        		}
	                        		finally {
		                        		ui.push();
	                        		}
	                        	}
	                        });
                        }                        
                    } catch (final Throwable t) {
                        try {
                        	Logger.getLogger(
                        			getClass().getName()).log(
                        					Level.SEVERE, "error", t); //$NON-NLS-1$
                        	if (ui != null) {
	                            ui.access(new Command() {
	                            	public void execute() {
	                            		try {
	                            			listener.error(t);
	                            		}
	                            		finally {
		                            		ui.push();
	                            		}
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
            		Level.SEVERE, "error", t);   //$NON-NLS-1$
            }
        }
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		return backgroundThread.scheduleWithFixedDelay(command, initialDelay,
				delay, unit);
	}
	
	public void shutdown() {
		backgroundThread.shutdown();
	}
}
