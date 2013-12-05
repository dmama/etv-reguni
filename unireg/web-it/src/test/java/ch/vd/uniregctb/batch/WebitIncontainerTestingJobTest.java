package ch.vd.uniregctb.batch;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.ubr.BatchRunnerClient;
import ch.vd.uniregctb.webservices.batch.JobDefinition;
import ch.vd.uniregctb.webservices.batch.JobStatut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Ce test permet de vérifier que l'exécution du test IcEvtCivilNaissanceTest est le même entre le context des tests unitaires et le context sous Tomcat.
 */
public class WebitIncontainerTestingJobTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(WebitIncontainerTestingJobTest.class);

	private static BatchRunnerClient client;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		if (client == null) {
			LOGGER.info("Connecting to: " + batchUrl + " with user = " + username);

			client = new BatchRunnerClient(batchUrl, username, password);
		}
	}

	@Test(timeout = 60000)
	// avec le temps d'initialisation d'Unireg (c'est un des premiers tests runnés), on a vu des exécutions qui prenaient 18 secondes.
	public void testRunBatch() throws Exception {

		final String jobName = "IT-InContainerTestingJob";

		// Démarre le job et attend qu'il soit terminé
		client.runBatch(jobName, null);

		final JobDefinition definition = client.getBatchDefinition(jobName);
		assertNotNull(definition);
		assertEquals(JobStatut.JOB_OK, definition.getStatut());
	}
}
