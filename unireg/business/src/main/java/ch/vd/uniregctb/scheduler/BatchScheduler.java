package ch.vd.uniregctb.scheduler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;

/**
 * Classe utilitaire pour gérer les batchs.
 */
public class BatchScheduler {

	private static final Logger LOGGER = Logger.getLogger(BatchScheduler.class);

	public static final String IMMEDIATE_TRIGGER = "ImmediateTrigger";

	private int triggerCount = 0;
	private Scheduler scheduler = null;
	private final HashMap<String, JobDefinition> jobs = new HashMap<String, JobDefinition>();

	public boolean isStarted() throws Exception {
		return !scheduler.isShutdown();
	}

	public void register(JobDefinition job) throws SchedulerException {

		if (job.getSortOrder() < 1) {
			throw new SchedulerException("Wrong sort order for job " + job.getName() + ": " + job.getSortOrder());
		}

		// Check that there is no job with the same sort order
		for (String key : jobs.keySet()) {
			JobDefinition value = jobs.get(key);
			if (value.getSortOrder() == job.getSortOrder()) {
				throw new SchedulerException("Duplicate sort order for job " + job.getName() + ": " + job.getSortOrder());
			}
		}

		jobs.put(job.getName(), job);
		scheduler.addJobListener(new UniregJobListener(job));
		LOGGER.info("Job <" + job.getName()+"> added.");
	}

	/**
	 * Enregistre un job comme devant être exécuté comme un cron.
	 *
	 * @param job            un job
	 * @param cronExpression l'expression cron (par exemple: "0 0/5 6-20 * * ?" pour exécuter le job toutes les 5 minutes, de 6h à 20h tous les jours)
	 * @throws SchedulerException en cas d'exception dans le scheduler
	 * @throws ParseException     en cas d'erreur dans la syntaxe de l'expression cron
	 */
	public void registerCron(JobDefinition job, String cronExpression) throws SchedulerException, ParseException {
		registerCron(job, job.getDefaultParams(), cronExpression);
	}

	/**
	 * Enregistre un job comme devant être exécuté comme un cron.
	 *
	 * @param job            un job
	 * @param params         les paramètres de démarrage du job
	 * @param cronExpression l'expression cron (par exemple: "0 0/5 6-20 * * ?" pour exécuter le job toutes les 5 minutes, de 6h à 20h tous les jours)
	 * @throws SchedulerException en cas d'exception dans le scheduler
	 * @throws ParseException     en cas d'erreur dans la syntaxe de l'expression cron
	 */
	public void registerCron(JobDefinition job, Map<String, Object> params, String cronExpression) throws SchedulerException, ParseException {

		AuthenticationHelper.pushPrincipal("[cron]");
		try {
			final Trigger trigger = new CronTrigger("CronTrigger-" + job.getName(), Scheduler.DEFAULT_GROUP, cronExpression);
			scheduleJob(job, params, trigger);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Démarre l'exécution d'un job avec les paramètres par défaut.
	 *
	 * @param jobName le nom du job à démarrer
	 * @return la définition du job
	 * @throws SchedulerException         en cas d'erreur de scheduling Quartz
	 * @throws JobAlreadyStartedException si le job est déjà démarré
	 */
	public JobDefinition startJobWithDefaultParams(String jobName) throws JobAlreadyStartedException, SchedulerException {

		LOGGER.info("Lancement du job <" + jobName + ">");
		JobDefinition job = jobs.get(jobName);
		Assert.notNull(job);
		HashMap<String, Object> params = job.getDefaultParams();
		return startJob(jobName, params);
	}

	/**
	 * Démarre l'exécution d'un job avec les paramètres spécifiés.
	 *
	 * @param jobName le nom du job à démarrer
	 * @param params  les paramètres du job
	 * @return la définition du job
	 * @throws SchedulerException         en cas d'erreur de scheduling Quartz
	 * @throws JobAlreadyStartedException si le job est déjà démarré
	 */
	public JobDefinition startJob(String jobName, Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {
		Assert.notNull(jobName, "Pas de nom de Job défini");

		LOGGER.info("Lancement du job <" + jobName + ">");
		JobDefinition job = jobs.get(jobName);
		Assert.notNull(job, "Le job <" + jobName + "> n'existe pas");
		return startJob(job, params);
	}

	private void scheduleJob(JobDefinition job, Map<String, Object> params, Trigger trigger) throws SchedulerException {

		// Renseignement de l'authentication
		final Authentication auth = AuthenticationHelper.getAuthentication();
		Assert.notNull(auth);
		trigger.getJobDataMap().put(JobDefinition.KEY_AUTH, auth);
		trigger.getJobDataMap().put(JobDefinition.KEY_PARAMS, params);

		// Création du lien entre le trigger et le détails du job
		registerJobDetailsIfNeeded(job);
		trigger.setJobName(job.getName());
		trigger.setJobGroup(Scheduler.DEFAULT_GROUP);

		// Ajout du trigger
		scheduler.scheduleJob(trigger);
	}

	private void registerJobDetailsIfNeeded(JobDefinition job) throws SchedulerException {

		JobDetail jobDetail = scheduler.getJobDetail(job.getName(), Scheduler.DEFAULT_GROUP);
		if (jobDetail == null) {

			jobDetail = new JobDetail(job.getName(), Scheduler.DEFAULT_GROUP, JobStarter.class);
			jobDetail.setDurability(true); // garde les détails du job après exécution

			final Authentication auth = AuthenticationHelper.getAuthentication();
			Assert.notNull(auth);

			jobDetail.getJobDataMap().put(JobDefinition.KEY_JOB, job);
			jobDetail.addJobListener(job.getName());

			scheduler.addJob(jobDetail, false);
		}
	}

	private JobDefinition startJob(JobDefinition job, Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {

		Assert.notNull(scheduler, "Le scheduler est NULL");
		Assert.isTrue(!scheduler.isShutdown(), "Le scheduler a été stoppé");

		if (job.isRunning()) {
			LOGGER.error("The job <" + job.getName() + "> is already started!");
			throw new JobAlreadyStartedException();
		}

		Audit.info("[Scheduler] Démarrage du job " + job.getName());

		// Construction d'un trigger qui se déclanche tout de suite
		final SimpleTrigger trigger = new SimpleTrigger(IMMEDIATE_TRIGGER + "-" + job.getName() + "-" + (triggerCount++), Scheduler.DEFAULT_GROUP);
		final Date scheduledDate = new Date();
		scheduleJob(job, params, trigger);

		// Attends que le job soit effectivement démarré
		while (job.getLastStart() == null || job.getLastStart().before(scheduledDate)) {
			sleep(50);
		}

		// Attends que le job soit fini, si on est en mode synchrone
		if (job.getSynchronousMode() == JobDefinition.JobSynchronousMode.SYNCHRONOUS) {
			waitForCompletion(job);
		}

		return job;
	}

	private void sleep(int millisecondes) {
		try {
			Thread.sleep(millisecondes);
		}
		catch (InterruptedException ignored) {
			LOGGER.warn("Timer exception ignored = " + ignored.toString());
		}
	}

	private void waitForCompletion(JobDefinition job) {
		int nbTimes = 0;
		while (job.isRunning()) {

			nbTimes++;
			if (LOGGER.isDebugEnabled() && nbTimes % 15 == 0) {
				LOGGER.debug("[SYNC] Attends que le job soit arrete...");
			}

			sleep(1000);
		}
	}

	public HashMap<String, JobDefinition> getJobs() {
		return jobs;
	}

	public JobDefinition getJob(String name) {
		return jobs.get(name);
	}

	/**
	 * Retourne la liste des jobs triés
	 *
	 * @return les jobs
	 */
	public List<JobDefinition> getSortedJobs() {
		ArrayList<JobDefinition> list = new ArrayList<JobDefinition>(jobs.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * Arrête l'exécution d'un job et ne retourne que lorsque le job est vraiment arrêté.
	 *
	 * @param name le nom du job à arrêter
	 * @throws SchedulerException en cas d'erreur de scheduling Quartz
	 */
	public void stopJob(String name) throws SchedulerException {

		final JobDefinition job = jobs.get(name);
		Assert.notNull(job, "Le job <" + name + "> n'existe pas");

		if (job.isRunning()) {
			LOGGER.info("Job <" + name + "> will be interrupted...");
			try {
				scheduler.interrupt(name, Scheduler.DEFAULT_GROUP);
			}
			catch (UnableToInterruptJobException e) {
				throw new SchedulerException("Unable to interrup job", e);
			}
		}

		// Attends que le job soit stoppé
		boolean warningDone = false;
		int count = 0;
		while (job.isRunning()) {

			sleep(500);
			
			count++;
			if (count > 10 && !warningDone) { // 5s
				LOGGER.warn("Job <" + name + "> takes looooong (>5s) to stop!");
				warningDone = true;
			}
			if (count > 60) { // 30s
				LOGGER.error("Job <" + name + "> takes REALLY too looooong (>30s) to stop. Aborting wait.");
				break;
			}
		}

		if (!job.isRunning()) {
			LOGGER.info("Job <" + name + "> is now stopped with status " + job.getStatut());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
}
