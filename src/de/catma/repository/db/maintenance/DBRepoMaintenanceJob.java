package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import de.catma.quartz.JobInstaller;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DBRepoMaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(DBRepoMaintenanceJob.class.getName());

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();

			DBRepositoryMaintainer dbRepositoryMaintainer = 
				new DBRepositoryMaintainer(
					dataMap.getString(
						JobInstaller.JobDataKey.PROPERTIES_PATH.name()));
			
			dbRepositoryMaintainer.run();
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing DBRepoMaintenanceJob", e);
		}
		
		
	}

}
