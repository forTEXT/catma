package de.catma.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.backgroundservice.ProgressListener;

public class UIBackgroundService implements BackgroundService {
	
	public static class UIProgressListener implements ProgressListener {
		
		private final ProgressListener delegate;
		private final UI ui;
		
		public UIProgressListener(ProgressListener delegate) {
			this.delegate = delegate;
			this.ui = UI.getCurrent();
		}

		@Override
		public void setProgress(String value, Object... args) {
			ui.access(() -> { 
				delegate.setProgress(value, args);
				UI.getCurrent().push();
			});
		}
	}

	private ScheduledExecutorService backgroundThread;
	private boolean background;
	
	public UIBackgroundService(boolean background) {
		this.background = background;
		if (background) {
			backgroundThread = Executors.newScheduledThreadPool(5);
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
                        if (ui != null && ui.isAttached()) {
	                        ui.access(new Runnable() {
	                        	public void run() {
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
                        	if (ui != null && ui.isAttached()) {
	                            ui.access(new Runnable() {
	                            	public void run() {
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
	
	public ScheduledFuture<?> schedule(Runnable command,
			long delay, TimeUnit unit) {
		return backgroundThread.schedule(command, delay, unit);
	}
	
	public void shutdown() {
		backgroundThread.shutdown();
	}
	
	@Override
	public ExecutorService getExecutorService(){
		return this.backgroundThread;
	}
}
