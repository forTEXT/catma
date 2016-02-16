package de.catma.repository.db.maintenance;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.quartz.JobInstaller;
import de.catma.quartz.JobInstaller.JobDataKey;
import de.catma.quartz.TriggerGroup;

public class DBMaintenanceInitializerServlet extends HttpServlet {
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			
			JobInstaller jobInstaller = new JobInstaller();
			
			int repoIndex = 1; // assume that the first configured repo is the local db repo

			JobDataMap repoJobDataMap = new JobDataMap();
			jobInstaller.install(
				DBRepoMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBRepoMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
//				.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
//				.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(30))
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 22-5 * * ?")
						.withMisfireHandlingInstructionDoNothing())
			    .build(),
			    repoJobDataMap);
			
			JobDataMap indexJobDataMap = new JobDataMap();

			indexJobDataMap.put(
					JobDataKey.SOURCEDOCIDXMAINTAIN.name(), 
					RepositoryPropertyKey.SourceDocumentIndexMaintainer.getIndexedValue(
							repoIndex));
			indexJobDataMap.put(
					JobDataKey.SOURCEDOCIDXMAINTAIN_MAXOBJ.name(), 
					RepositoryPropertyKey.SourceDocumentIndexMaintainerMaxObjects.getIndexedValue(
							repoIndex));
			indexJobDataMap.put(
					JobDataKey.DBIDXMAINTAINMAXOBJECTCOUNT.name(), 
					RepositoryPropertyKey.DBIndexMaintainerMaxObjects.getIndexedValue(
							repoIndex, 10));
			
			jobInstaller.install(
				DBIndexMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBIndexMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
				.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(
					RepositoryPropertyKey.DBIndexMaintenanceJobIntervalInSeconds.getIndexedValue(
							repoIndex, 180))
				.withMisfireHandlingInstructionIgnoreMisfires())
				
//				.withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 22-5 * * ?")
//						.withMisfireHandlingInstructionDoNothing())
				
//				.withSchedule(CronScheduleBuilder.cronSchedule("30 0/3 * * * ?")
//						.withMisfireHandlingInstructionDoNothing())
			    .build(),
			    indexJobDataMap);
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
		
	}
}
