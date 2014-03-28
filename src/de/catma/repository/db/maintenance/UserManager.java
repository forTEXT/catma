package de.catma.repository.db.maintenance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.catma.repository.LoginToken;

public class UserManager {
	private static final long TWELVEHOURS_IN_MILLISECONDS = 43200000L;
	private static AtomicInteger userCount = new AtomicInteger(0);
	private static Lock loginLock = new ReentrantLock();
	private static WeakHashMap<LoginToken, Long> loginEvents = new WeakHashMap<LoginToken, Long>();
	
	public void incrementUserCount(LoginToken loginToken) {
		loginLock.lock();
		try {
			loginEvents.put(loginToken, new Date().getTime());
			userCount.incrementAndGet();
		}
		finally {
			loginLock.unlock();
		}
	}

	public void decrementUserCount(LoginToken loginToken) {
		loginLock.lock();
		try {
			loginEvents.remove(loginToken);
			userCount.decrementAndGet();
		}
		finally {
			loginLock.unlock();
		}
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
	
	public void clearStaleLoginTokens() {
		loginLock.lock();
		try {
			ArrayList<LoginToken> staleLoginTokens = new ArrayList<LoginToken>();
			for (Map.Entry<LoginToken, Long> entry : loginEvents.entrySet()) {
				LoginToken loginToken = entry.getKey();
				long timestamp = entry.getValue();
				long now = System.currentTimeMillis();
				if ((timestamp-now) > TWELVEHOURS_IN_MILLISECONDS) {
					staleLoginTokens.add(loginToken);
				}
			}
			
			for (LoginToken loginToken : staleLoginTokens) {
				loginToken.close();
			}
		}
		finally {
			loginLock.unlock();
		}
	}
}
