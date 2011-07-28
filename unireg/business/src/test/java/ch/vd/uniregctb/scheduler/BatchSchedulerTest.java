package ch.vd.uniregctb.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.JobTest;

import static ch.vd.uniregctb.scheduler.JobDefinition.JobStatut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class BatchSchedulerTest extends JobTest {

	private final static Logger LOGGER = Logger.getLogger(BatchSchedulerTest.class);

	private BatchScheduler batchScheduler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		assertTrue("Le scheduler n'est pas starté!", batchScheduler.isStarted());
	}

	private void assertNotRunning(JobDefinition job) {
		assertNotSame(JobStatut.JOB_RUNNING, job.getStatut());
	}

	@Test(timeout = 10000)
	public void testJob() throws Exception {
		LOGGER.debug("Begin testJob method.");

		final JobDefinition job = batchScheduler.getJob(LoggingJob.NAME);
		assertEquals(JobStatut.JOB_OK, job.getStatut());

		// Démarrage du job
		final Date statTime = new Date();
		batchScheduler.startJob(LoggingJob.NAME, null);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, statTime);

		// Attente de la fin de l'exécution
		int count = 0;
		while (job.isRunning()) {

			Thread.sleep(100); // 100ms
			count++;

			if (count > 20) { // 2s
				batchScheduler.stopJob(LoggingJob.NAME);
				LOGGER.info("Message: " + job.getRunningMessage() + " Statut=" + job.getStatut());
				assertNotRunning(job);
				assertEquals(JobStatut.JOB_INTERRUPTED, job.getStatut());
			}
		}

		// Vérification que le job ne tourne plus
		assertNotRunning(job);
		LOGGER.debug("End testJob method.");
	}

	@Test(timeout = 10000)
	public void testDoubleStart() throws Exception {
		LOGGER.debug("Begin testDoubleStart method.");

		final HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(LoggingJob.I_DELAY, 2000); // assez long pour qu'il tourne encore lors du deuxième démarrage et de l'interruption 
		final Date startTime = new Date();
		final JobDefinition job = batchScheduler.startJob(LoggingJob.NAME, map);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, startTime);

		try {
			// Double start
			batchScheduler.startJob(LoggingJob.NAME, null);
			fail("Job started 2 times without error");
		}
		catch (JobAlreadyStartedException e) {
			assertTrue("OK : Job NOT started 2 times ", job.isRunning());
		}

		batchScheduler.stopJob(LoggingJob.NAME);

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(100); // 100ms
		}

		assertEquals(JobStatut.JOB_INTERRUPTED, job.getStatut());
		assertFalse("Job still started!", job.isRunning());
		
		LOGGER.debug("End testDoubleStart method.");
	}

	@Test(timeout = 10000)
	public void testStartStopStart() throws Exception {

		LOGGER.debug("Begin testStartStopStart method.");

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(LoggingJob.I_DELAY, 200);

		{
			final Date startTime = new Date();
			JobDefinition job = batchScheduler.startJob(LoggingJob.NAME, map);

			// Attente du démarrage de l'exécution
			waitUntilRunning(job, startTime);

			batchScheduler.stopJob(LoggingJob.NAME);

			// Attente de l'arrêt de l'exécution
			while (job.isRunning()) {
				Thread.sleep(100); // 100ms
			}

			assertNotRunning(job);
		}

		{
			final Date startTime = new Date();
			JobDefinition job = batchScheduler.startJob(LoggingJob.NAME, map);

			// Attente du démarrage de l'exécution
			waitUntilRunning(job, startTime);

			batchScheduler.stopJob(LoggingJob.NAME);

			// Attente de l'arrêt de l'exécution
			while (job.isRunning()) {
				Thread.sleep(100); // 100ms
			}

			assertNotRunning(job);
		}
		
		LOGGER.debug("End testStartStopStart method.");
	}


	@Test(timeout = 10000)
	public void testStartExceptionThrown() throws Exception {

		LOGGER.info("### Test: testStartExceptionThrown");

		final Date startTime = new Date();
		final JobDefinition job = batchScheduler.startJob(ExceptionThrowingJob.NAME, null);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, startTime);

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(100); // 100ms
		}

		assertEquals(JobStatut.JOB_EXCEPTION, job.getStatut());
		assertFalse("Job still started!", job.isRunning());
	}

	/**
	 * Démarre 2 jobs différents, mais en même temps.
	 */
	@Test(timeout = 10000)
	public void testStart2jobs() throws Exception {

		// Job1: Exception
		final Date startTime = new Date();
		JobDefinition job1 = batchScheduler.startJob(ExceptionThrowingJob.NAME, null);

		// Job2 : Logging
		HashMap<String, Object> loggingParams = new HashMap<String, Object>();
		loggingParams.put(LoggingJob.I_DELAY, 200); // 200ms de delay
		JobDefinition job2 = batchScheduler.startJob(LoggingJob.NAME, loggingParams);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job1, startTime);
		waitUntilRunning(job2, startTime);

		// Attente de l'arrêt de l'exécution
		while (job1.isRunning()) {
			Thread.sleep(100); // 100ms
		}
		// Attente de l'arrêt de l'exécution
		while (job2.isRunning()) {
			Thread.sleep(100); // 100ms
		}

		assertEquals(JobStatut.JOB_EXCEPTION, job1.getStatut());
		assertFalse("Job still started!", job1.isRunning());
		assertEquals(JobStatut.JOB_OK, job2.getStatut());
		assertFalse("Job still started!", job2.isRunning());
	}

	@Test(timeout = 10000)
	public void testCronJob() throws Exception {

		// Construit une expression cron pour faire démarrer le batch dans trois secondes (= temps d'initialisation maximum estimé entre l'enregistrement d'un job dans Quartz et son démarrage) 
		final Date startTime = new Date();
		final Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.SECOND, 3);
		final String cron = String.format("%d %d %d * * ?", cal.get(Calendar.SECOND), cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY));
		LOGGER.debug("cron = \"" + cron + "\"");

		// Schedule le cron
		final JobDefinition job = batchScheduler.getJob(LoggingJob.NAME);
		batchScheduler.registerCron(job, cron);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, startTime);

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(100); // 100ms
		}

		assertEquals(JobStatut.JOB_OK, job.getStatut());
	}

	/**
	 * Vérifie qu'un job qui a reçu une demande d'interruption reste bien 'running' tant qu'il ne s'est pas réellement interrompu.
	 */
	@Test
	public void testStartAndInterruptJob() throws Exception {

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(LoggingJob.I_DELAY, 500); // 10 * 0.5 seconde
		map.put(LoggingJob.I_INT_DELAY, 2000); // 2 secondes

		// Démarrage du job
		final Date startTime = new Date();
		JobDefinition job = batchScheduler.startJob(LoggingJob.NAME, map);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, startTime);

		// Interruption du job (il va mettre 2 secondes à terminer son traitement)
		job.interrupt();

		// On attend que le batch scheduler ait transmis l'événement d'interruption au job
		while (job.getStatut() == JobStatut.JOB_RUNNING) {
			Thread.sleep(100); // 100ms
		}

		// On vérifie que le job est toujours running (car il est entrain de s'interrompre)
		assertTrue(job.isRunning());
		assertTrue(job.isInterrupted());

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(100); // 100ms
		}

		// Le job doit être dans l'état interrompu
		assertNotRunning(job);
		assertEquals(JobStatut.JOB_INTERRUPTED, job.getStatut());
	}
}
