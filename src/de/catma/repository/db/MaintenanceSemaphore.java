package de.catma.repository.db;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class MaintenanceSemaphore {

	public enum Type {
		CLEANING,
		IMPORT,
		SYNCH,
		;
	}
	
	private final static ReentrantLock CLEANING_LOCK = new ReentrantLock();
	private final static AtomicInteger IMPORT_SYNCH_COUNT = new AtomicInteger();
	
	private Type semType;
	private boolean access = false;
	private Logger logger = Logger.getLogger(MaintenanceSemaphore.class.getName());
	
	public MaintenanceSemaphore(Type semType) {
		this.semType = semType;
		createAccess();
	}

	private void createAccess() {
		logger.info("trying to get access for type " + semType);
		
		if (semType.equals(Type.CLEANING)) {
			CLEANING_LOCK.lock();
			if (IMPORT_SYNCH_COUNT.get() > 0) {
				CLEANING_LOCK.unlock();
				logger.info("could not get access because imports or synchs are running");
			}
			else {
				access = true;
				logger.info("access aquired for type " + semType);
			}
		}
		else {
			IMPORT_SYNCH_COUNT.incrementAndGet();
			if (CLEANING_LOCK.isLocked()) {
				logger.info("could not get access because a cleaning is running, waiting...");
				CLEANING_LOCK.lock();
				logger.info("got cleaning lock to signal import");
				CLEANING_LOCK.unlock();
			}
			access = true;
			logger.info("access aquired for type " + semType 
					+ " current value " + IMPORT_SYNCH_COUNT.get());
		}
	}

	public boolean hasAccess() {
		return access;
	}
	
	public void release() {
		if (access) {
			if (semType.equals(Type.CLEANING)) {
				CLEANING_LOCK.unlock();
				logger.info("semaphore released for type " + semType);
			}
			else {
				IMPORT_SYNCH_COUNT.decrementAndGet();
				logger.info("semaphore released for type " + semType 
						+ " current value " + IMPORT_SYNCH_COUNT.get());
			}
		}
	}
}
