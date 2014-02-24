package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.catma.quartz.JobInstaller;

public class MaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(MaintenanceJob.class.getName());

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			new DBMaintainer((String)ctx.getJobDetail().getJobDataMap().get(
					JobInstaller.JobDataKey.PROPERTIES_PATH.name())).run();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing HeurecleaExporterJob", e);
		}
		
		
	}

}
