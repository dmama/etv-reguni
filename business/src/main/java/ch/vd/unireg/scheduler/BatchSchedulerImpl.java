package ch.vd.uniregctb.scheduler;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.stats.JobMonitor;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Classe utilitaire pour gérer les batchs.
 */
public class BatchSchedulerImpl implements BatchScheduler, InitializingBean, DisposableBean, DynamicMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchSchedulerImpl.class);

	public static final String IMMEDIATE_TRIGGER = "ImmediateTrigger";

	private StatsService statsService;

	private Scheduler scheduler = null;

	private int timeoutOnStopAll = 5;       // en minutes, le temps d'attente maximal lors d'un appel à stopAllRunningJobs()

	private final AtomicInteger triggerCount = new AtomicInteger(0);
	private final Map<String, JobDefinition> jobs = new HashMap<>();

	@Override
	public boolean isStarted() throws SchedulerException {
		return !scheduler.isShutdown();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (timeoutOnStopAll <= 0) {
			throw new IllegalArgumentException("La valeur du timeout (minutes) doit être strictement positive");
		}
		scheduler.getListenerManager().addJobListener(UniregJobListener.INSTANCE);
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	/**
	 * Implémentation locale du job monitor
	 */
	private static class JobMonitorImpl implements JobMonitor {

		private final JobDefinition job;

		private JobMonitorImpl(JobDefinition job) {
			this.job = job;
		}

		@Override
		public Date getStartDate() {
			return job.isRunning() ? job.getLastStart() : null;
		}

		@Override
		public Integer getPercentProgression() {
			return job.getPercentProgression();
		}

		@Override
		public String getRunningMessage() {
			return job.getRunningMessage();
		}
	}

	@Override
	public void register(JobDefinition job) throws SchedulerException {

		final String jobName = job.getName();
		if (job.getSortOrder() < 1) {
			throw new SchedulerException("Wrong sort order for job " + jobName + ": " + job.getSortOrder());
		}

		// Check that there is no job with the same sort order
		for (Map.Entry<String, JobDefinition> stringJobDefinitionEntry : jobs.entrySet()) {
			final JobDefinition value = stringJobDefinitionEntry.getValue();
			if (value.getSortOrder() == job.getSortOrder()) {
				throw new SchedulerException("Duplicate sort order for job " + jobName + ": " + job.getSortOrder());
			}
		}

		jobs.put(jobName, job);
		statsService.registerJobMonitor(jobName, new JobMonitorImpl(job));
		LOGGER.info("Job <" + jobName +"> added.");
	}

	/**
	 * Enregistre un job comme devant être exécuté comme un cron.
	 *
	 * @param job            un job
	 * @param cronExpression l'expression cron (par exemple: "0 0/5 6-20 * * ?" pour exécuter le job toutes les 5 minutes, de 6h à 20h tous les jours)
	 * @throws SchedulerException en cas d'exception dans le scheduler
	 * @throws ParseException     en cas d'erreur dans la syntaxe de l'expression cron
	 */
	@Override
	public void registerCron(JobDefinition job, String cronExpression) throws SchedulerException, ParseException {
		registerCron(job, null, cronExpression);
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
	@Override
	public void registerCron(JobDefinition job, @Nullable Map<String, Object> params, String cronExpression) throws SchedulerException, ParseException {

		AuthenticationHelper.pushPrincipal("[cron]");
		try {
			final Trigger trigger = TriggerBuilder.newTrigger()
					.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
					.withIdentity(new TriggerKey("CronTrigger-" + job.getName(), Scheduler.DEFAULT_GROUP))
					.forJob(new JobKey(job.getName(), Scheduler.DEFAULT_GROUP))
					.build();
			scheduleJob(job, params, trigger);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	public void destroy() throws Exception {
		for (String jobName : jobs.keySet()) {
			statsService.unregisterJobMonitor(jobName);
		}
		jobs.clear();
		scheduler.getListenerManager().removeJobListener(UniregJobListener.INSTANCE.getName());
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
	@Override
	public JobDefinition startJob(String jobName, @Nullable Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {
		Assert.notNull(jobName, "Pas de nom de Job défini");

		LOGGER.info("Lancement du job <" + jobName + '>');
		JobDefinition job = jobs.get(jobName);
		Assert.notNull(job, "Le job <" + jobName + "> n'existe pas");
		return startJob(job, params);
	}

	private void scheduleJob(JobDefinition job, @Nullable Map<String, Object> params, Trigger trigger) throws SchedulerException {

		// Renseignement de l'authentication
		final String launchingUser = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(launchingUser);
		trigger.getJobDataMap().put(JobDefinition.KEY_USER, launchingUser);
		trigger.getJobDataMap().put(JobDefinition.KEY_PARAMS, params);

		// Création du lien entre le trigger et le détails du job
		registerJobDetailsIfNeeded(job);

		// Ajout du trigger
		scheduler.scheduleJob(trigger);
	}

	private void registerJobDetailsIfNeeded(JobDefinition job) throws SchedulerException {

		final JobKey jobKey = new JobKey(job.getName(), Scheduler.DEFAULT_GROUP);
		JobDetail jobDetail = scheduler.getJobDetail(jobKey);
		if (jobDetail == null) {
			final JobDataMap map = new JobDataMap();
			map.put(JobDefinition.KEY_JOB, job);
			jobDetail = JobBuilder.newJob(JobStarter.class)
					.withIdentity(jobKey)
					.storeDurably(true)
					.usingJobData(map)
					.build();

			scheduler.addJob(jobDetail, false);
		}
	}

	private JobDefinition startJob(JobDefinition job, @Nullable Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {

		Assert.notNull(scheduler, "Le scheduler est NULL");
		Assert.isTrue(!scheduler.isShutdown(), "Le scheduler a été stoppé");

		if (job.isRunning()) {
			LOGGER.error("The job <" + job.getName() + "> is already started!");
			throw new JobAlreadyStartedException();
		}

		// Construction d'un trigger qui se déclenche tout de suite
		final SimpleTrigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(new TriggerKey(String.format("%s-%s-%d", IMMEDIATE_TRIGGER, job.getName(), triggerCount.incrementAndGet()), Scheduler.DEFAULT_GROUP))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.forJob(new JobKey(job.getName(), Scheduler.DEFAULT_GROUP))
				.startNow()
				.build();
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

	private void sleep(long millisecondes) {
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

	@Override
	public Map<String, JobDefinition> getJobs() {
		return jobs;
	}

	@Override
	public JobDefinition getJob(String name) {
		return jobs.get(name);
	}

	/**
	 * Retourne la liste des jobs triés
	 *
	 * @return les jobs
	 */
	@Override
	public List<JobDefinition> getSortedJobs() {
		ArrayList<JobDefinition> list = new ArrayList<>(jobs.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * Arrête l'exécution d'un job et ne retourne que lorsque le job est vraiment arrêté.
	 *
	 * @param name le nom du job à arrêter
	 * @param timeout (optionel) si fourni, ne rend la main qu'après que le job est vraiment arrêté ou que le timeout soit écoulé ; si absent, retour immédiat
	 * @throws SchedulerException en cas d'erreur de scheduling Quartz
	 */
	@Override
	public void stopJob(String name, @Nullable Duration timeout) throws SchedulerException {

		final JobDefinition job = jobs.get(name);
		Assert.notNull(job, "Le job <" + name + "> n'existe pas");

		// demande d'arrêt
		registerInterruptionRequest(job);

		// Attends que le job soit stoppé
		if (timeout != null && job.isRunning()) {

			// on attend deux phases...
			// - à 20% du timeout, on indique un warning
			// - à 100% du timeout, on indique une erreur et on sort
			final long millis = Math.max(timeout.toMillis(), 5);            // minimum 5ms pour que 20% soit non-zéro

			// états terminaux
			final Set<JobDefinition.JobStatut> etatsTerminaux = EnumSet.of(JobDefinition.JobStatut.JOB_EXCEPTION,
			                                                               JobDefinition.JobStatut.JOB_INTERRUPTED,
			                                                               JobDefinition.JobStatut.JOB_OK);

			try {
				// on attend 20% du temps ...
				try {
					job.waitForStatusIn(etatsTerminaux, Duration.ofMillis(millis / 5));
				}
				catch (JobDefinition.TimeoutExpiredException e) {
					LOGGER.warn("Job <" + name + "> takes looooong (> " + millis / 5 + " ms) to stop!");

					// .. et si le job n'est toujours pas arrêté, on attend les 80% restants
					try {
						final JobDefinition.JobStatut statutFinal = job.waitForStatusIn(etatsTerminaux, Duration.ofMillis(millis * 4 / 5));
						LOGGER.info("Job <" + name + "> is now stopped with status " + statutFinal);
					}
					catch (JobDefinition.TimeoutExpiredException e1) {
						LOGGER.error("Job <" + name + "> takes REALLY too looooong (> " + millis + " ms) to stop. Aborting wait.");
					}
				}
			}
			catch (InterruptedException e) {
				// c'est fini, on s'en va...
				LOGGER.error("Thread d'attente de la fin du job <" + name + "> interrompu.", e);
			}
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTimeoutOnStopAll(int timeoutOnStopAll) {
		this.timeoutOnStopAll = timeoutOnStopAll;
	}

	/**
	 * Fait une demande d'interruption du batch s'il est en cours
	 * @param job job à arrêter
	 * @return <code>true</code> si le batch tournait effectivement, et <code>false</code> sinon
	 * @throws SchedulerException en cas d'impossibilité de demander l'interruption du job
	 */
	private boolean registerInterruptionRequest(JobDefinition job) throws SchedulerException {
		final boolean isRunning = job.isRunning();
		if (isRunning) {
			final String name = job.getName();
			LOGGER.info("Job <" + name + "> will be interrupted...");
			try {
				scheduler.interrupt(new JobKey(name, Scheduler.DEFAULT_GROUP));
			}
			catch (UnableToInterruptJobException e) {
				throw new SchedulerException("Unable to interrupt job", e);
			}

		}
		return isRunning;
	}

	/**
	 * Demande à tous les jobs encore en cours de s'arrêter et leur laisse 5 minutes (max) pour ce faire
	 */
	@Override
	public boolean stopAllRunningJobs() {

		boolean tousMorts = true;

		// demande d'arrêt pour tous ceux qui tournent encore
		final List<JobDefinition> arretDemande = new LinkedList<>();
		for (JobDefinition job : jobs.values()) {
			try {
				if (registerInterruptionRequest(job)) {
					arretDemande.add(job);
				}
			}
			catch (SchedulerException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		// on attend le temps qu'il faut (mais pas plus de "timeoutOnStopAll" minutes !) pour que tous les batchs encore
		// en cours s'arrêtent proprement
		final long maxWait = timeoutOnStopAll * 60000L;
		final long startWait = System.currentTimeMillis();
		while (!arretDemande.isEmpty()) {

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ça a l'air sérieux... il faut sortir, je crois...
				LOGGER.error("Thread interrompu...", e);
				break;
			}

			// on prend en compte les arrêts intervenus depuis le dernier pointage
			arretDemande.removeIf(job -> !job.isRunning());

			final long now = System.currentTimeMillis();
			if (now - startWait > maxWait) {
				if (!arretDemande.isEmpty()) {
					final StringBuilder b = new StringBuilder();
					b.append("Le ou les jobs suivants semblent ne pas vouloir s'arrêter (tant pis pour eux !) :\n");
					for (JobDefinition job : arretDemande) {
						b.append('\t').append(job.getName()).append('\n');
					}
					LOGGER.warn(b.toString());

					tousMorts = false;
				}

				// on sort, tout est fini, on a fait ce qu'on a pu
				break;
			}
		}

		return tousMorts;
	}

	private static String buildStatusString(JobDefinition job) {
		final StringBuilder b = new StringBuilder();
		if (job.getLastStart() != null) {
			b.append(job.getStatut());
		}
		if (job.isRunning()) {
			b.append(", running since ").append(job.getLastStart());
			final Integer percentProgression = job.getPercentProgression();
			if (percentProgression != null) {
				b.append(" (").append(percentProgression).append("% completion)");
			}
			final String runningMessage = job.getRunningMessage();
			if (StringUtils.isNotBlank(runningMessage)) {
				b.append(": ").append(runningMessage);
			}
		}
		else if (job.getLastEnd() != null) {
			b.append(", ended on ").append(job.getLastEnd());
		}
		else {
			b.append("never run yet");
		}
		return b.toString();
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		final JobDefinition def = jobs.get(attribute);
		if (def == null) {
			throw new AttributeNotFoundException();
		}
		else {
			return buildStatusString(def);
		}
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new NotImplementedException();
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		final AttributeList list = new AttributeList(attributes.length);
		for (String attribute : attributes) {
			final JobDefinition def = jobs.get(attribute);
			if (def != null) {
				list.add(new Attribute(attribute, buildStatusString(def)));
			}
			else {
				list.add(new Attribute(attribute, "Unknown attribute"));
			}
		}
		return list;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		throw new NotImplementedException();
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		try {
			if (actionName.startsWith("stop")) {
				final String msg;
				final String target = actionName.substring(4);
				if ("ALL".equals(target)) {
					if (!stopAllRunningJobs()) {
						msg = "Some jobs could not be stopped!";
					}
					else {
						msg = "Any running job is now stopped";
					}
				}
				else {
					final JobDefinition job = jobs.get(target);
					if (job.isRunning()) {
						stopJob(target, Duration.ofSeconds(30));
						if (job.isRunning()) {
							msg = "Job is still running!";
						}
						else {
							msg = "Job was stopped successfully";
						}
					}
					else {
						msg = "Job is not even running!";
					}
				}
				return msg;
			}
			else {
				throw new NoSuchMethodException(actionName);
			}
		}
		catch (SchedulerException e) {
			throw new MBeanException(e);
		}
		catch (NoSuchMethodException e) {
			throw new ReflectionException(e);
		}
	}

	@Override
	public MBeanInfo getMBeanInfo() {

		final List<JobDefinition> shownJobs = new ArrayList<>(jobs.size());
		for (JobDefinition job : jobs.values()) {
			if (job.isVisible()) {
				shownJobs.add(job);
			}
		}

		// on trie la liste par ordre alphabetique du nom du batch
		// (car jconsole montre les attributs par ordre alphabétique quel que soit l'ordre d'apparition
		// dans le tableau fourni ici)
		shownJobs.sort(Comparator.comparing(JobDefinition::getName));
		final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[shownJobs.size()];
		final MBeanOperationInfo[] operations = new MBeanOperationInfo[shownJobs.size() + 1];

		// opération pour demander l'interruption de tous les jobs en cours
		operations[0] = new MBeanOperationInfo("stopALL", "Causes interruption of all running jobs", null, "String", MBeanOperationInfo.ACTION);

		// pour chacun des batches, on expose un attribut virtuel qui expose l'avancement du job
		// ainsi qu'une méthode pour demander l'interruption du batch
		for (int i = 0 ; i < attrs.length ; ++ i) {
			final JobDefinition job = shownJobs.get(i);
			attrs[i] = new MBeanAttributeInfo(job.getName(), "job", job.getDescription(), true, false, false);
			operations[i+1] = new MBeanOperationInfo(String.format("stop%s", job.getName()), String.format("Interrupts job %s", job.getName()), null, "String", MBeanOperationInfo.ACTION);
		}

		return new MBeanInfo(getClass().getName(), "Batch scheduler", attrs, null, operations, null);
	}
}
