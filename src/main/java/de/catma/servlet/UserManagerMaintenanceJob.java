package de.catma.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.catma.repository.db.maintenance.UserManager;

@DisallowConcurrentExecution
public final class UserManagerMaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(UserManagerMaintenanceJob.class.getName());

	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		try {
			UserManager userManager = new UserManager();
			
			userManager.clearStaleLoginTokens();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error executing UserManagerMaintenanceJob", e);
		}		
	}
}