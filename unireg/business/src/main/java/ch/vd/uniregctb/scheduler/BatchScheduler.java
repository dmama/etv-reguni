package ch.vd.uniregctb.scheduler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.acegisecurity.Authentication;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.DisposableBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;

/**
 * Classe utilitaire pour gérer des batchs
 *
 * @author xcifde
 *
 */
public class BatchScheduler implements DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(BatchScheduler.class);

	public static final String IMMEDIATE_TRIGGER = "ImmediateTrigger";

	private Scheduler scheduler = null;
	private final HashMap<String, JobDefinition> jobs = new HashMap<String, JobDefinition>();

	public boolean isStarted() throws Exception {
		return !scheduler.isShutdown();
	}

	public void destroy() throws Exception {
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
		LOGGER.info("Job added: " + job.getName());
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

		AuthenticationHelper.pushPrincipal("[cron]");
		try {
			final Authentication auth = AuthenticationHelper.getAuthentication();
			Assert.notNull(auth);

			final JobDetail jobDetail = new JobDetail(job.getName(), Scheduler.DEFAULT_GROUP, JobStarter.class);
			jobDetail.getJobDataMap().put(JobDefinition.KEY_JOB, job);
			jobDetail.getJobDataMap().put(JobDefinition.KEY_AUTH, auth);
			jobDetail.getJobDataMap().put(JobDefinition.KEY_PARAMS, job.getDefaultParams());

			final Trigger trigger = new CronTrigger("CronTrigger-" + job.getName(), Scheduler.DEFAULT_GROUP, cronExpression);
			scheduler.scheduleJob(jobDetail, trigger);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Execute l'indexation avec les params par défaut
	 *
	 * @param jobName
	 * @param params
	 * @return
	 * @throws JobAlreadyStartedException
	 * @throws SchedulerException
	 */
	public JobDefinition startJobWithDefaultParams(String jobName) throws JobAlreadyStartedException, SchedulerException {

		LOGGER.info("Lancement du job: " + jobName);
		JobDefinition job = jobs.get(jobName);
		Assert.notNull(job);
		HashMap<String, Object> params = job.getDefaultParams();
		return startJob(jobName, params);
	}

	/**
	 * Execute l'indexation des contribuables
	 *
	 * @throws SchedulerException
	 */
	public JobDefinition startJob(String jobName, HashMap<String, Object> params) throws JobAlreadyStartedException, SchedulerException {
		Assert.notNull(jobName, "Pas de nom de Job défini");

		LOGGER.info("Lancement du job: " + jobName);
		JobDefinition job = jobs.get(jobName);
		Assert.notNull(job, "Le job '"+jobName+"' n'existe pas");
		return startJob(job, params);
	}

	/**
	 * Factorisation de code: Permet de démarrer un job en mode immediat de façon standard
	 *
	 * @param job
	 * @param params
	 * @throws JobAlreadyStartedException
	 * @throws SchedulerException
	 */
	private void setUpAndStartJob(JobDefinition job, HashMap<String, Object> params) throws JobAlreadyStartedException, SchedulerException {

		Assert.notNull(scheduler, "Le scheduler est NULL");
		Assert.isTrue(!scheduler.isShutdown(), "Le scheduler a été stoppé");

		if (job.isRunning()) {
			LOGGER.error("The job " + job.getName() + " is already started!");
			throw new JobAlreadyStartedException();
		}

		Audit.info("[Scheduler] Démarrage du job " + job.getName());

		JobDetail jobDetail = new JobDetail(job.getName(), Scheduler.DEFAULT_GROUP, JobStarter.class);
		// Fais en sorte que le job soit supprimé du Scheduler après execution
		jobDetail.setDurability(false);
		jobDetail.setVolatility(true);

		Authentication auth = AuthenticationHelper.getAuthentication();
		Assert.notNull(auth);

		jobDetail.getJobDataMap().put(JobDefinition.KEY_JOB, job);
		jobDetail.getJobDataMap().put(JobDefinition.KEY_AUTH, auth);
		jobDetail.getJobDataMap().put(JobDefinition.KEY_PARAMS, params);

		// Construction d'un trigger qui est declanche tout de suite
		SimpleTrigger trigger = new SimpleTrigger(IMMEDIATE_TRIGGER + "-" + job.getName(), Scheduler.DEFAULT_GROUP);

		// Ajout du job au scheduler
		jobDetail.addJobListener(job.getName());

		while (scheduler.getJobDetail(job.getName(), Scheduler.DEFAULT_GROUP) != null) {
			// si on arrive ici c'est que le job précédent ne tourne effectivement plus (c'est asserté plus haut) *mais* que - pour des raisons de scheduling
			// de la JVM - le quartz scheduler n'a pas encore pu supprimer les détails associé au job. On lui laisse donc un peu de temps pour le faire.
			sleep(50);
		}

		scheduler.scheduleJob(jobDetail, trigger);
	}

	private void sleep(int millis) throws SchedulerException {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			throw new SchedulerException(e);
		}
	}

	private JobDefinition startJob(JobDefinition job, HashMap<String, Object> params) throws JobAlreadyStartedException, SchedulerException {

		setUpAndStartJob(job, params);

		// Attends que le job soit fini
		// si on est en mode synchrone
		int nbTimes = 0;
		while (job.getSynchronousMode() == JobDefinition.JobSynchronousMode.SYNCHRONOUS && job.isRunning()) {

			nbTimes++;
			if (nbTimes % 15 == 0) {
				LOGGER.debug("[SYNC] Attends que le job soit arrete...");
			}

			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ignored) {
				LOGGER.warn("Timer exception ignored = " + ignored.toString());
			}
		}

		return job;
	}

	/**
	 * Retourne la liste des jobs
	 *
	 * @return les jobs
	 */
	public HashMap<String, JobDefinition> getJobs() {
		return jobs;
	}

	/**
	 * Retourne la liste des jobs
	 *
	 * @return les jobs
	 */
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
	 * Stoppe l'indexation des contribuables
	 *
	 * @return Seulement quand le job est vraiment stoppé
	 * @throws SchedulerException
	 */
	public void stopJob(String name) throws SchedulerException {

		LOGGER.info("Job " + name + " will be interrupted...");
		try {
			scheduler.interrupt(name, Scheduler.DEFAULT_GROUP);
		}
		catch (UnableToInterruptJobException e) {
			throw new SchedulerException("Unable to interrup job", e);
		}

		// Attends que le job soit stoppé
		boolean warningDone = false;
		int count = 0;
		boolean exit = false;
		JobDefinition job = jobs.get(name);
		while (job.isRunning() && !exit) {

			try {
				Thread.sleep(500);
			}
			catch (InterruptedException ignored) {
				LOGGER.warn("Timer exception ignored = " + ignored.toString());
			}
			count++;
			if (count > 10 && !warningDone) { // 5s
				LOGGER.warn("Job " + name + " takes looooong (>5s) to stop!");
				warningDone = true;
			}
			if (count > 60) { // 30s
				LOGGER.error("Job " + name + " takes REALLY too looooong (>5s) to stop. Aborting wait.");
				exit = true;
			}
		}
		if (!job.isRunning()) {
			LOGGER.info("Job " + name + " is now stopped.");
		}
	}

	/**
	 * @param scheduler
	 *            the scheduler to set
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
