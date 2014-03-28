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
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put(JobInstaller.JobDataKey.PROPERTIES_PATH.name(), 
					config.getServletContext().getRealPath("catma.properties"));
			
			JobInstaller jobInstaller = new JobInstaller();
			
			jobInstaller.install(
				DBRepoMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBRepoMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
//				.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 30).withMisfireHandlingInstructionDoNothing())
//				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInSeconds(60))
			    .build(),
				jobDataMap);
			
			jobInstaller.install(
				DBIndexMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
						DBIndexMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
//					.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 30).withMisfireHandlingInstructionDoNothing())
//					.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInSeconds(60))
			    .build(),
				new JobDataMap());
		}
		catch (SchedulerException se) {
			throw new ServletException(se);
		}
		
	}
}
