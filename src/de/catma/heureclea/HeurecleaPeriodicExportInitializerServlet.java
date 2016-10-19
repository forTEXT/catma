package de.catma.heureclea;


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

@Deprecated
public class HeurecleaPeriodicExportInitializerServlet extends HttpServlet {
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
//			JobDataMap jobDataMap = new JobDataMap();

			JobInstaller jobInstaller = new JobInstaller();
			jobInstaller.deinstall(HeurecleaExporterJob.class);
//			jobInstaller.install(
//				HeurecleaExporterJob.class,
//				
//				TriggerBuilder.newTrigger()
//				.withIdentity(TriggerKey.triggerKey(
//						HeurecleaExporterJob.class.getName()+"_Trigger",
//		    			TriggerGroup.DEFAULT.name()))
//				.startNow()
////				.withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount(1))
//				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 30).withMisfireHandlingInstructionDoNothing())
////				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInSeconds(60))
//			    .build(),
//				jobDataMap);
		}
		catch (SchedulerException se) {
			throw new ServletException(se);
		}
		
	}
}
