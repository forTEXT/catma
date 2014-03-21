package de.catma.repository.db.maintenance;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {

	private static AtomicInteger userCount = new AtomicInteger(0);
	private static Lock loginLock = new ReentrantLock();
	
	public void incrementUserCount() {
		loginLock.lock();
		try {
			userCount.incrementAndGet();
		}
		finally {
			loginLock.unlock();
		}
	}

	public void decrementUserCount() {
		userCount.decrementAndGet();
	}
	
	public int getUserCount() {
		return userCount.get();
	}
	
	public void lockLogin() {
		loginLock.lock();
	}

	public void unlockLogin() {
		loginLock.unlock();
	}
	
}
