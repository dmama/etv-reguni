package ch.vd.uniregctb.scheduler;

import java.util.HashMap;

import org.acegisecurity.Authentication;
import org.apache.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;

public class JobStarter implements Job, InterruptableJob {

	private final Logger LOGGER = Logger.getLogger(JobStarter.class);

	public final static String KEY_JOB = "job";
	public final static String KEY_AUTH = "authentication";
	public final static String KEY_PARAMS = "params";

	private Authentication authentication;
	private JobDefinition job;

	/**
	 * Initialise le context de sécurité Acegi avec les rôles donnés en paramètres
	 */
	private void initializeSecurityContext() {
		Assert.notNull(authentication, "L'authentification en peut pas être nulle");
		AuthenticationHelper.setAuthentication(authentication);
	}

	protected void terminate() throws Exception {
		AuthenticationHelper.resetAuthentication();
	}

	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext ctxt) throws JobExecutionException {

		final JobDataMap dataMap = ctxt.getJobDetail().getJobDataMap();

		job = (JobDefinition) dataMap.get(KEY_JOB);
		authentication = (Authentication) dataMap.get(KEY_AUTH);
		final HashMap<String, Object> params = (HashMap<String, Object>) dataMap.get(KEY_PARAMS);

		try {
			job.initialize();

			if (params == null || params.isEmpty()) {
				LOGGER.info("Démarrage du job " + job.getName() + " sans paramètre");
			}
			else {
				LOGGER.info("Démarrage du job " + job.getName() + " avec les paramètres " + params.toString());
			}
			initializeSecurityContext();

			try {
				job.execute(params);
			}
			finally {
				job.terminate();
			}

			LOGGER.info("Job " + job.getName() + " finished: " + job.getStatut() + " " + job);

			terminate();
		}
		catch (Exception e) {
			LOGGER.error("Job execution exception: " + e.getMessage(), e);
			job.setStatut(JobDefinition.JobStatut.JOB_EXCEPTION);
			job.setRunningMessage(e.getMessage());
		}
		catch (Error e) {
			LOGGER.fatal("Job execution error: " + e.getMessage(), e);
			job.setStatut(JobDefinition.JobStatut.JOB_EXCEPTION);
			job.setRunningMessage(e.getMessage());
			throw e;
		}

		// Si le job s'est terminé correctement, on supprime le message
		if (job.getStatut() == JobDefinition.JobStatut.JOB_OK) {
			job.setRunningMessage("");
		}
	}

	public void interrupt() throws UnableToInterruptJobException {
		job.interrupt();
	}
}
