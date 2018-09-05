package de.catma.servlet;



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

public class UserManagerMaintenanceServlet extends HttpServlet {
	
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
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 4,12,20 * * ?")
						.withMisfireHandlingInstructionDoNothing())
//				.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(30))
			    .build(),
				new JobDataMap());

		}
		catch (SchedulerException se) {
			throw new ServletException(se);
		}
		
	}
}
