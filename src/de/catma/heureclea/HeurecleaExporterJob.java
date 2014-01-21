package de.catma.heureclea;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import de.catma.quartz.JobInstaller;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HeurecleaExporterJob implements Job {

	private Logger logger = Logger.getLogger(HeurecleaExporterJob.class.getName());
	
	public void execute(JobExecutionContext ctx)
			throws JobExecutionException {
		try {
			new HeurecleaExporter((String)ctx.getJobDetail().getJobDataMap().get(
					JobInstaller.JobDataKey.PROPERTIES_PATH.name())).export();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error executing HeurecleaExporterJob", e);
		}
	}
}