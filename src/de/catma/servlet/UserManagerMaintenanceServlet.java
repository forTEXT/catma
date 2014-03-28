package de.catma.servlet;


import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import de.catma.quartz.JobInstaller;
import de.catma.quartz.TriggerGroup;
import de.catma.repository.db.maintenance.UserManager;

public class UserManagerMaintenanceServlet extends HttpServlet {
	
	public final static class UserManagerMaintenanceJob implements Job {
		
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
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			JobInstaller jobInstaller = new JobInstaller();
			
			jobInstaller.install(
				UserManagerMaintenanceJob.class,
				TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(
					UserManagerMaintenanceJob.class.getName()+"_Trigger",
		    			TriggerGroup.DEFAULT.name()))
				.startNow()
//				.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 30).withMisfireHandlingInstructionDoNothing())
//				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInSeconds(60))
			    .build(),
				new JobDataMap());

		}
		catch (SchedulerException se) {
			throw new ServletException(se);
		}
		
	}
}
