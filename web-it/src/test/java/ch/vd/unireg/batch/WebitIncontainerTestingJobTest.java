package ch.vd.uniregctb.batch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.ubr.BatchRunnerClient;
import ch.vd.uniregctb.ubr.JobDescription;
import ch.vd.uniregctb.ubr.JobStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Ce test permet de vérifier que l'exécution du test IcEvtCivilNaissanceTest est le même entre le context des tests unitaires et le context sous Tomcat.
 */
public class WebitIncontainerTestingJobTest extends WebitTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebitIncontainerTestingJobTest.class);

	private BatchRunnerClient client;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		LOGGER.info("Connecting to: " + batchUrl + " with user = " + username);
		client = new BatchRunnerClient(batchUrl, username, password);
	}

	@Test(timeout = 60000)
	// avec le temps d'initialisation d'Unireg (c'est un des premiers tests runnés), on a vu des exécutions qui prenaient 18 secondes.
	public void testRunBatch() throws Exception {

		final String jobName = "IT-InContainerTestingJob";

		// Démarre le job et attend qu'il soit terminé
		client.runBatch(jobName, null);

		final JobDescription definition = client.getBatchDescription(jobName);
		assertNotNull(definition);
		assertEquals(JobStatus.OK, definition.getStatus());
	}
}
