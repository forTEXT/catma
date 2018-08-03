package de.catma.repository.db.executionshield;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutionShield {
	private int MYSQL_DEADLOCK_ERR = 1213;
	
	private final Logger logger = Logger.getLogger(ExecutionShield.class.getName());
	
	public <T> T execute(DBOperation<T> dbOperation) throws IOException {
		int execCount = 1;
		Exception lastException = null;
		
		while (execCount <= 5) {
			try {
				return dbOperation.execute();
			}
			catch (Exception e) {
				lastException = e;
				handleException(execCount, e);
				execCount++;
			}
		}
		
		throw new IOException(lastException);
	}

	private void handleException(int execCount, Exception e) throws IOException {
		if (exceptionChainContainsShieldedException(e)) {
			logger.log(
					Level.WARNING, 
					"deadlock detected, shielded execution, execCount: " + execCount, 
					e);
			Random r = new Random(new Date().getTime());
			int sleepTime = r.nextInt(2000);			
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ie) {
				// try again
			}
		}
		else {
			throw new IOException(e);
		}
	}

	private boolean exceptionChainContainsShieldedException(Exception e) {
		Throwable t = e;
		int depth = 0; // just in case
		while ((t != null) && !(t instanceof SQLException) && depth < 100) {
			t = t.getCause();
			depth++;
		}
		
		if (t==null) {
			return false;
		}
		else if (t instanceof SQLException) {
			SQLException sqlException = (SQLException) t;
			if (sqlException.getErrorCode() == MYSQL_DEADLOCK_ERR) {
				return true;
			}
		}
		
		return false;
	}

}
