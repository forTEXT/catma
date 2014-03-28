package de.catma.quartz;

import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.SchedulerRepository;

public class JobInstaller {
	public static enum JobDataKey {
		PROPERTIES_PATH,
		REPO_TAGREF_OFFSET,
		REPO_PROP_OFFSET,
		IDX_TAGREF_OFFSET,
		IDX_PROP_OFFSET,
		FILE_CLEAN_OFFSET,
		;
	}
	
	private Logger logger = Logger.getLogger(JobInstaller.class.getName());
	public JobInstaller() {
	}

	
	public void install(
			Class<? extends Job> jobClass, 
			Trigger jobTrigger, JobDataMap jobData) throws SchedulerException {
        SchedulerRepository schedRep = SchedulerRepository.getInstance();
    	
        Scheduler scheduler = schedRep.lookup("CATMAQuartzScheduler");
        
        TriggerKey triggerKey = TriggerKey.triggerKey(
        		jobClass.getName()+"_Trigger",
    			TriggerGroup.DEFAULT.name());
        
        Trigger trigger = scheduler.getTrigger(triggerKey);
    	JobDetail jobDetail = null; 

        if (trigger == null) {
        	logger.info("installing " + jobClass.getSimpleName() + "...");
        	
        	jobDetail = 
        		JobBuilder.newJob(jobClass)
        		.withIdentity(
        			jobClass.getName(),
        			JobGroup.DEFAULT.name())
        		.setJobData(jobData)
        		.build();
        	
        	trigger = jobTrigger.getTriggerBuilder().forJob(jobDetail).build();
        	
        	scheduler.scheduleJob(jobDetail, trigger);
        	logger.info(jobClass.getSimpleName() + " installed!");
        }
        else {
        	logger.info(jobClass.getSimpleName() + " is already intalled.");

        	trigger = jobTrigger.getTriggerBuilder().build();
        	scheduler.rescheduleJob(triggerKey, trigger);
        	logger.info(jobClass.getSimpleName() + " rescheduled!");
        }
	}
	
}
