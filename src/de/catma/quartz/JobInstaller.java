package de.catma.quartz;

import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.SchedulerRepository;

public class JobInstaller {
	
	public static enum JobDataKey {
		REPO_TAGREF_OFFSET,
		REPO_PROP_OFFSET,
		IDX_TAGREF_OFFSET,
		IDX_PROP_OFFSET,
		FILE_CLEAN_OFFSET, 
		SOURCEDOCIDXMAINTAIN, 
		SOURCEDOCIDXMAINTAIN_MAXOBJ, 
		SOURCEDOCIDXMAINTAIN_OFFSET,
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

        if (trigger == null) {
        	logger.info("installing " + jobClass.getSimpleName() + "...");
        	
        	JobDetail jobDetail = 
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
        	logger.info(jobClass.getSimpleName() + " is already installed.");

        	JobDetail jobDetail = scheduler.getJobDetail(
        			JobKey.jobKey(jobClass.getName(), JobGroup.DEFAULT.name()));
        	
        	for (JobDataKey key : JobDataKey.values()) {
        		if (jobData.containsKey(key.name())) {
        			jobDetail.getJobDataMap().put(key.name(), jobData.get(key.name()));
        		}
        		else if (jobDetail.getJobDataMap().containsKey(key.name())){
        			jobDetail.getJobDataMap().remove(key.name());
        		}
        	}
        	scheduler.addJob(jobDetail, true, true);
        	
        	trigger = jobTrigger.getTriggerBuilder().build();
        	scheduler.rescheduleJob(triggerKey, trigger);
        	logger.info(jobClass.getSimpleName() + " rescheduled!");
        }
	}
	
}
