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

		final String initialThreadName = Thread.currentThread().getName();

		try {
			// on donne le nom du job au thread d'exécution, de manière à le repérer plus facilement
			Thread.currentThread().setName(job.getName());

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
		finally {
			Thread.currentThread().setName(initialThreadName);
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		// [SIFISC-4311] Le job peut être null si le job n'a pas pu être démarré pour une raison ou une autre. Exemple :
		//   1) démarrage du job d'indexation FULL pour un utilisateur -> une instance de JobStarter est créée
		//   2) à 2 heures du matin, démarrage du job d'indexation incrémental pendant que le job démarré en 1) tourne toujours -> une autre instance
		//      de JobStarter est créée, mais le job DatabaseIndexJob lève une exception car il est encore à l'état RUNNING. La deuxième instance
		//      du Jobstarter reste cependant enregistrée dans Quartz.
		//   3) l'utilisateur décide d'interrompre le job d'indexation FULL -> les deux instances de JobStarter vont recevoir un appel à 'interrupt',
		//      mais seule la première instance possède un job défini.
		// => dans le cas où le job est null, il n'y a rien à interrompre et il suffit d'ignorer l'appel.
		if (job != null) {
			job.interrupt();
		}
	}
}
