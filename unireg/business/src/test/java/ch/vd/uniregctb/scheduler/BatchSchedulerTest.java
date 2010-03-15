package ch.vd.uniregctb.scheduler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;

import ch.vd.uniregctb.common.BusinessTest;

public class BatchSchedulerTest extends BusinessTest {

	private final static Logger LOGGER = Logger.getLogger(BatchSchedulerTest.class);

	private BatchScheduler batchScheduler;

	public BatchSchedulerTest() {
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		assertTrue("Le scheduler n'est pas starté!", batchScheduler.isStarted());
	}

	private void assertNotRunning(JobDefinition job) {

		assertTrue( "Name: "+job.getName()+" Job status: "+job.getStatut(),
					job.getStatut() != JobDefinition.JobStatut.JOB_READY &&
					job.getStatut() != JobDefinition.JobStatut.JOB_RUNNING);
	}

	private void assertRunning(JobDefinition job) {

		assertTrue( "Name: "+job.getName()+" Job status: "+job.getStatut(),
					job.getStatut() == JobDefinition.JobStatut.JOB_RUNNING);
	}

	private void assertReadyOrRunning(JobDefinition job) {

		assertTrue(	"Name: "+job.getName()+" Job status: "+job.getStatut(),
					job.getStatut() == JobDefinition.JobStatut.JOB_READY ||
					job.getStatut() == JobDefinition.JobStatut.JOB_RUNNING);
	}

	private void assertReadyOrRunningOrOk(JobDefinition job) {

		assertTrue(	"Name: "+job.getName()+" Job status: "+job.getStatut(),
					job.getStatut() == JobDefinition.JobStatut.JOB_READY ||
					job.getStatut() == JobDefinition.JobStatut.JOB_RUNNING ||
					job.getStatut() == JobDefinition.JobStatut.JOB_OK );
	}

	@Test
@NotTransactional
	public void testJob() throws Exception {
		LOGGER.debug("Begin testJob method.");
		String name = LoggingJob.NAME;
		JobDefinition job = batchScheduler.startJob(name, null);
		assertReadyOrRunningOrOk(job);

		int count = 0;
		while (job.isRunning()) {

			Thread.sleep(100); // 100ms
			count++;

			if (count > 20) { // 2s
				batchScheduler.stopJob(name);
				LOGGER.info("Message: "+job.getRunningMessage()+" Statut="+job.getStatut());
				assertNotRunning(job);
				assertEquals(JobDefinition.JobStatut.JOB_INTERRUPTED, job.getStatut());
			}
		}
		LOGGER.debug("End testJob method.");
	}

	// FIXME (msi) execution differente entre Windows et Unix
	@Ignore
	@Test
	@NotTransactional
	public void testDoubleStart() throws Exception {
		LOGGER.debug("Begin testDoubleStart method.");
		String name = LoggingJob.NAME;

		JobDefinition job = batchScheduler.startJob(name, null);
		assertEquals(name, job.getName());
		assertReadyOrRunning(job);

		// Attends que le job ait démarré pour pouvoir le re-starter puis le stopper
		while (job.getStatut() != JobDefinition.JobStatut.JOB_RUNNING) {
			Thread.sleep(50);
		}
		assertRunning(job);

		try {
			// Double start
			job = batchScheduler.startJob(name, null);
			fail("Job started 2 times without error");
		}
		catch (JobAlreadyStartedException e) {
			assertTrue("OK : Job NOT started 2 times ", job.isRunning());
		}

		batchScheduler.stopJob(name);
		assertNotRunning(job);
		assertEquals(JobDefinition.JobStatut.JOB_INTERRUPTED, job.getStatut());
		assertFalse("Job still started!", job.isRunning());
		LOGGER.debug("End testDoubleStart method.");
	}

	@Test
	@NotTransactional
	public void testStartStopStart() throws Exception {
		LOGGER.debug("Begin testStartStopStart method.");
		String name = LoggingJob.NAME;
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("delay", new Integer(200));

		{
			JobDefinition job = batchScheduler.startJob(name, map);
			assertReadyOrRunningOrOk(job);

			batchScheduler.stopJob(name);
			assertNotRunning(job);
		}

		{
			JobDefinition job = batchScheduler.startJob(name, map);
			assertReadyOrRunningOrOk(job);

			batchScheduler.stopJob(name);
			assertNotRunning(job);
		}
		LOGGER.debug("End testStartStopStart method.");
	}


	@Test
	@NotTransactional
	public void testStartExceptionThrown() throws Exception {

		LOGGER.info("### Test: testStartExceptionThrown");

		String name = ExceptionThrowingJob.NAME;
		JobDefinition job = batchScheduler.startJob(name, null);

		// Attends l'exception
		int count = 0;
		while (count < 10 && job.isRunning()) {

			LOGGER.debug("Attente de la fin du job");
			Thread.sleep(500);
			count++;
		}

		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());
		assertFalse("Job still started!", job.isRunning());
	}

	/**
	 * Démarre 2 jobs différents, mais en même temps.
	 *
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void testStart2jobs() throws Exception {

		// Job1: Exception
		JobDefinition job1 = batchScheduler.startJobWithDefaultParams(ExceptionThrowingJob.NAME);

		// Job2 : Logging
		HashMap<String, Object> loggingParams = new HashMap<String, Object>();
		loggingParams.put(LoggingJob.I_DELAY, new Integer(200)); // 200ms de delay
		JobDefinition job2 = batchScheduler.startJob(LoggingJob.NAME, loggingParams);

		// Attends l'exception
		int count = 0;
		while (count < 10 &&
				(job1.isRunning() || job2.isRunning())) {

			LOGGER.debug("Attente de la fin du job");
			Thread.sleep(2000);
			count++;
		}

		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job1.getStatut());
		assertFalse("Job still started!", job1.isRunning());
		assertEquals(JobDefinition.JobStatut.JOB_OK, job2.getStatut());
		assertFalse("Job still started!", job2.isRunning());
	}

}
