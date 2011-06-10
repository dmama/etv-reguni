package ch.vd.uniregctb.scheduler;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.springframework.security.core.Authentication;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;

public class JobStarter implements Job, InterruptableJob {

	private final Logger LOGGER = Logger.getLogger(JobStarter.class);

	private Authentication authentication;
	private JobDefinition job;

	/**
	 * Initialise le context de sécurité avec les rôles donnés en paramètres
	 */
	private void initializeSecurityContext() {
		Assert.notNull(authentication, "L'authentification en peut pas être nulle");
		AuthenticationHelper.setAuthentication(authentication);
	}

	protected void terminate() throws Exception {
		AuthenticationHelper.resetAuthentication();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext ctxt) throws JobExecutionException {

		final JobDataMap jobData = ctxt.getJobDetail().getJobDataMap();
		final JobDataMap triggerData = ctxt.getTrigger().getJobDataMap();

		job = (JobDefinition) jobData.get(JobDefinition.KEY_JOB);
		authentication = (Authentication) triggerData.get(JobDefinition.KEY_AUTH);
		final HashMap<String, Object> params = (HashMap<String, Object>) triggerData.get(JobDefinition.KEY_PARAMS);

		try {
			job.initialize();

			if (!job.isLogDisabled()) {
				if (params == null || params.isEmpty()) {
					LOGGER.info("Démarrage du job <" + job.getName() + "> sans paramètre");
				}
				else {
					LOGGER.info("Démarrage du job <" + job.getName() + "> avec les paramètres " + params.toString());
				}
			}
			initializeSecurityContext();

			try {
				job.execute(params);
			}
			finally {
				job.terminate();
			}

			terminate();
		}
		catch (Exception e) {
			LOGGER.error("Job <" + job.getName() + "> exception: " + e.getMessage(), e);
			job.setStatut(JobDefinition.JobStatut.JOB_EXCEPTION);
			job.setRunningMessage(e.getMessage());
		}
		catch (Error e) {
			LOGGER.fatal("Job <" + job.getName() + "> error: " + e.getMessage(), e);
			job.setStatut(JobDefinition.JobStatut.JOB_EXCEPTION);
			job.setRunningMessage(e.getMessage());
			throw e;
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		job.interrupt();
	}
}
