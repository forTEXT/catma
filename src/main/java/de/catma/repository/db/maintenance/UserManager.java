package de.catma.repository.db.maintenance;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import de.catma.repository.LoginToken;

public class UserManager {
	private static final long TWELVEHOURS_IN_MILLISECONDS = 43200000L;
	private static Lock loginLock = new ReentrantLock();
	private static WeakHashMap<LoginToken, Long> loginEvents = new WeakHashMap<LoginToken, Long>();
	private Logger logger = Logger.getLogger(UserManager.class.getName());
	
	public void login(LoginToken loginToken) {
		loginLock.lock();
		try {
			loginEvents.put(loginToken, new Date().getTime());
			logger.info("user" + loginToken.getUser() + " has been added to user count (" + getUserCount() + ")" );
		}
		finally {
			loginLock.unlock();
		}
	}

	public void logout(LoginToken loginToken) {
		loginLock.lock();
		try {
			loginEvents.remove(loginToken);
			logger.info("user" + loginToken.getUser() + " has been substracted from user count (" + getUserCount() + ")" );
		}
		finally {
			loginLock.unlock();
		}
	}
	
	public int getUserCount() {
		return loginEvents.size();
	}
	
	public void lockLogin() {
		loginLock.lock();
	}

	public void unlockLogin() {
		loginLock.unlock();
	}
	
	public void clearStaleLoginTokens() {
		logger.info("checking stale login tokens");
		
		loginLock.lock();
		try {
			ArrayList<LoginToken> staleLoginTokens = new ArrayList<LoginToken>();
			for (Map.Entry<LoginToken, Long> entry : loginEvents.entrySet()) {
				LoginToken loginToken = entry.getKey();
				long timestamp = entry.getValue();
				long now = System.currentTimeMillis();
				if ((timestamp-now) > TWELVEHOURS_IN_MILLISECONDS) {
					logger.info("found stale login token " 
							+ loginToken.getUser() 
							+ " logged in since " + new Date(timestamp));
					staleLoginTokens.add(loginToken);
				}
			}
			
			for (LoginToken loginToken : staleLoginTokens) {
				loginToken.close();
			}
		}
		finally {
			loginLock.unlock();
			logger.info("finished checking stale login tokens");
		}
	}
}
