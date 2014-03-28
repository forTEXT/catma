package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.catma.quartz.JobInstaller;

public class DBRepoMaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(DBRepoMaintenanceJob.class.getName());

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();
			int fileCleanOffset = 0;
			if (dataMap.containsKey(JobInstaller.JobDataKey.FILE_CLEAN_OFFSET.name())) {
				fileCleanOffset = 
					dataMap.getInt(JobInstaller.JobDataKey.FILE_CLEAN_OFFSET.name());
			}
			DBRepositoryMaintainer dbRepositoryMaintainer = 
				new DBRepositoryMaintainer(
					dataMap.getString(
						JobInstaller.JobDataKey.PROPERTIES_PATH.name()),
					fileCleanOffset);
			
			dbRepositoryMaintainer.run();
			
			dataMap.put(
				JobInstaller.JobDataKey.FILE_CLEAN_OFFSET.name(), 
				dbRepositoryMaintainer.getFileCleanOffset());
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing HeurecleaExporterJob", e);
		}
		
		
	}

}
