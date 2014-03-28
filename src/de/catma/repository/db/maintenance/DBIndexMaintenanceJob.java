package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.catma.quartz.JobInstaller;

public class DBIndexMaintenanceJob implements Job {
	
	private Logger logger = Logger.getLogger(DBIndexMaintenanceJob.class.getName());

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();
			
			int repoTagReferenceRowOffset = 0; 
			int repoPropertyRowOffset = 0;     
			int indexTagReferenceRowOffset = 0;
			int indexPropertyRowOffset = 0;    
			
			if (dataMap.containsKey(JobInstaller.JobDataKey.REPO_TAGREF_OFFSET.name())) {
				repoTagReferenceRowOffset = 
						dataMap.getInt(JobInstaller.JobDataKey.REPO_TAGREF_OFFSET.name());
			}
			if (dataMap.containsKey(JobInstaller.JobDataKey.REPO_PROP_OFFSET.name())) {
				repoPropertyRowOffset = 
						dataMap.getInt(JobInstaller.JobDataKey.REPO_PROP_OFFSET.name());
			}
			if (dataMap.containsKey(JobInstaller.JobDataKey.IDX_TAGREF_OFFSET.name())) {
				indexTagReferenceRowOffset = 
						dataMap.getInt(JobInstaller.JobDataKey.IDX_TAGREF_OFFSET.name());
			}
			if (dataMap.containsKey(JobInstaller.JobDataKey.IDX_PROP_OFFSET.name())) {
				indexPropertyRowOffset = 
						dataMap.getInt(JobInstaller.JobDataKey.IDX_PROP_OFFSET.name());
			}
			
			DBIndexMaintainer dbIndexMaintainer = 
					new DBIndexMaintainer(
						repoTagReferenceRowOffset, repoPropertyRowOffset, 
						indexTagReferenceRowOffset, indexPropertyRowOffset);
			
			dbIndexMaintainer.run();
			
			dataMap.put(
					JobInstaller.JobDataKey.REPO_TAGREF_OFFSET.name(), 
					dbIndexMaintainer.getRepoTagReferenceRowOffset());
			dataMap.put(
					JobInstaller.JobDataKey.REPO_PROP_OFFSET.name(), 
					dbIndexMaintainer.getRepoPropertyRowOffset());
			dataMap.put(
					JobInstaller.JobDataKey.IDX_TAGREF_OFFSET.name(), 
					dbIndexMaintainer.getIndexTagReferenceRowOffset());
			dataMap.put(
					JobInstaller.JobDataKey.IDX_PROP_OFFSET.name(), 
					dbIndexMaintainer.getIndexPropertyRowOffset());

		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing HeurecleaExporterJob", e);
		}
		
		
	}

}
