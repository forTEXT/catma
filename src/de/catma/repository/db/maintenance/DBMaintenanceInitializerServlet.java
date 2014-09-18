package de.catma.repository.db.maintenance;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import de.catma.quartz.JobInstaller;
import de.catma.quartz.TriggerGroup;

public class DBMaintenanceInitializerServlet extends HttpServlet {
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			
			JobInstaller jobInstaller = new JobInstaller();
			
			
			JobDataMap repoJobDataMap = new JobDataMap();
			jobInstaller.install(
				DBRepoMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBRepoMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
//				.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
//				.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(10))
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 22-5 * * ?")
						.withMisfireHandlingInstructionDoNothing())
			    .build(),
			    repoJobDataMap);
			
			JobDataMap indexJobDataMap = new JobDataMap();
			jobInstaller.install(
				DBIndexMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBIndexMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
				.withSchedule(CronScheduleBuilder.cronSchedule("30 0/3 * * * ?")
						.withMisfireHandlingInstructionDoNothing())
			    .build(),
			    indexJobDataMap);
		}
		catch (SchedulerException se) {
			throw new ServletException(se);
		}
		
	}
}
