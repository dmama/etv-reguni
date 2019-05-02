package ch.vd.unireg.scheduler;

import java.util.Map;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.utils.LogLevel;

public class JobStarter implements Job, InterruptableJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobStarter.class);

	private volatile JobDefinition job;

	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext ctxt) throws JobExecutionException {

		final JobDataMap dataMap = ctxt.getMergedJobDataMap();
		final String launchingUser = (String) dataMap.get(JobDefinition.KEY_USER);
		final Map<String, Object> params = (Map<String, Object>) dataMap.get(JobDefinition.KEY_PARAMS);

		job = (JobDefinition) dataMap.get(JobDefinition.KEY_JOB);
		try {
			executeJob(launchingUser, params);
		}
		finally {
			job = null;
		}
	}

	private void executeJob(String launchingUser, Map<String, Object> params) {
		final String initialThreadName = Thread.currentThread().getName();
		try {
			// on donne le nom du job au thread d'exécution, de manière à le repérer plus facilement
			Thread.currentThread().setName(job.getName());

			if (!job.isLogDisabled()) {
				if (params == null || params.isEmpty()) {
					LOGGER.info("Démarrage du job <" + job.getName() + "> sans paramètre");
				}
				else {
					LOGGER.info("Démarrage du job <" + job.getName() + "> avec les paramètres " + params.toString());
				}
			}

			AuthenticationHelper.pushPrincipal(launchingUser);
			try {
				job.initialize();
				try {
					job.execute(params);
				}
				finally {
					job.terminate();
				}
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
		catch (Exception e) {
			LOGGER.error("Job <" + job.getName() + "> exception: " + e.getMessage(), e);
			job.setStatut(JobDefinition.JobStatut.JOB_EXCEPTION);
			job.setRunningMessage(e.getMessage());
		}
		catch (Error e) {
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, "Job <" + job.getName() + "> error: " + e.getMessage(), e);
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
