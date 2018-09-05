package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DBRepoMaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(DBRepoMaintenanceJob.class.getName());

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			DBRepositoryMaintainer dbRepositoryMaintainer = 
				new DBRepositoryMaintainer();
			
			dbRepositoryMaintainer.run();
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing DBRepoMaintenanceJob", e);
		}
		
		
	}

}
