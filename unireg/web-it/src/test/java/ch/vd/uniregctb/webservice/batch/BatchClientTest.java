package ch.vd.uniregctb.webservice.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.ubr.BatchRunnerClient;
import ch.vd.uniregctb.webservices.batch.BatchWSException;
import ch.vd.uniregctb.webservices.batch.JobDefinition;
import ch.vd.uniregctb.webservices.batch.JobStatut;
import ch.vd.uniregctb.webservices.batch.Param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class BatchClientTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(BatchClientTest.class);

	private static final String BATCH_NAME = "IT-BatchClientJob";

	private static BatchRunnerClient client;

	private final String aujourdhui = RegDateHelper.dateToDisplayString(RegDate.get());

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		if (client == null) {
			LOGGER.info("Connecting to: " + batchUrl + " with user = " + username);

			client = new BatchRunnerClient(batchUrl, username, password);
		}
	}

	@Test
	public void testGetListJobs() throws Exception {

		final List<String> list = client.getBatchNames();
		assertNotNull(list);
		assertEquals(45, list.size());
		int i = 0;
		assertEquals("IT-BatchClientJob", list.get(i++));
		assertEquals("IT-InContainerTestingJob", list.get(i++));
		assertEquals("DumpAssujettissementsJob", list.get(i++));
		assertEquals("DatabaseIndexerJob", list.get(i++));
		assertEquals("OptimizeIndexJob", list.get(i++));
		assertEquals("CheckCoherenceIndexerJob", list.get(i++));
		assertEquals("OfficeImpotIndexerJob", list.get(i++));
		assertEquals("RapprocherCtbRegistreFoncierJob", list.get(i++));
		assertEquals("DeterminerMouvementsDossiersEnMasseJob", list.get(i++));
		assertEquals("UpdateTacheStatsJob", list.get(i++));
		assertEquals("EvenementCivilHandlerJob", list.get(i++));
		assertEquals("IdentifierContribuableJob", list.get(i++));
		assertEquals("EvenementExterneHandlerJob", list.get(i++));
		assertEquals("EditiqueListeRecapJob", list.get(i++));
		assertEquals("EditiqueSommationLRJob", list.get(i++));
		assertEquals("ReinitialiserBaremeDoubleGainJob", list.get(i++));
		assertEquals("DeterminerLRsEchuesJob", list.get(i++));
		assertEquals("DetermineDIsEnMasseJob", list.get(i++));
		assertEquals("EnvoiDIsEnMasseJob", list.get(i++));
		assertEquals("EditiqueSommationDIJob", list.get(i++));
		assertEquals("EchoirDIsJob", list.get(i++));
		assertEquals("ImpressionChemisesTOJob", list.get(i++));
		assertEquals("ExclureContribuablesEnvoiJob", list.get(i++));
		assertEquals("DemandeDelaiCollectiveJob", list.get(i++));
		assertEquals("OuvertureForsContribuableMajeurJob", list.get(i++));
		assertEquals("FusionDeCommunesJob", list.get(i++));
		assertEquals("ProduireRolesCommuneJob", list.get(i++));
		assertEquals("ProduireRolesOIDJob", list.get(i++));
		assertEquals("ProduireStatsJob", list.get(i++));
		assertEquals("ListeDINonEmisesJob", list.get(i++));
		assertEquals("ListesNominativesJob", list.get(i++));
		assertEquals("AcomptesJob", list.get(i++));
		assertEquals("ExtractionAfcJob", list.get(i++));
		assertEquals("ValidationJob", list.get(i++));
		assertEquals("ListeTachesEnIstanceParOIDJob", list.get(i++));
		assertEquals("ListeCtbsResidentsSansForVdJob", list.get(i++));
		assertEquals("StatistiquesEvenementsJob", list.get(i++));
		assertEquals("DumpDatabaseJob", list.get(i++));
		assertEquals("LoadDatabaseJob", list.get(i++));
		assertEquals("RamasseDocumentJob", list.get(i++));
		assertEquals("UpdateSequencesJob", list.get(i++));
		assertEquals("DumpTiersListJob", list.get(i++));
		assertEquals("CorrectionForsHCJob", list.get(i++));
		assertEquals("CorrectionFlagHabitantJob", list.get(i++));
		assertEquals("CacheResetJob", list.get(i++));
	}

	@Test
	public void testStartJobWithNoBatchName() throws Exception {

		try {
			client.startBatch("", null);
			fail("il ne devrait pas être possible de pouvoir démarrer un batch nul");
		}
		catch (BatchWSException e) {
			assertEquals("Batch Name incorrect", e.getMessage());
		}
	}

	@Test
	public void testStartJobWithBadBatchName() throws Exception {

		try {
			client.startBatch("inconnu", null);
			fail("il ne devrait pas être possible de pouvoir démarrer un batch inconnu");
		}
		catch (BatchWSException e) {
			assertEquals("Batch Name incorrect", e.getMessage());
		}
	}

	@Test
	public void testStartJobWithBatchName() throws Exception {

		client.startBatch(BATCH_NAME, null);

		// on attend maintenant la fin du job avant de continuer
		client.stopBatch(BATCH_NAME);
	}

	@Test
	public void testStartJobWithBatchNameWithArguments() throws Exception {

		Map<String, Object> args = new HashMap<String, Object>(2);
		args.put("dateDebut", "2008-03-20");
		args.put("count", "12");
		client.startBatch(BATCH_NAME, args);

		// on attend maintenant la fin du job avant de continuer
		client.stopBatch(BATCH_NAME);
	}

	@Test
	public void testStopJobWithBatchName() throws Exception {

		// démarre le job avec le paramètre duration = 10 secondes, de telle manière qui'il dure assez longtemps pour qu'on puisse l'arrêter
		Map<String, Object> args = new HashMap<String, Object>(1);
		args.put("duration", "10");
		client.startBatch(BATCH_NAME, args);

		// on attend maintenant la fin du job avant de continuer
		client.stopBatch(BATCH_NAME);

		// le job doit être dans l'état interrompu
		final JobDefinition definition = client.getBatchDefinition(BATCH_NAME);
		assertNotNull(definition);
		assertEquals(JobStatut.JOB_INTERRUPTED, definition.getStatut());
	}

	@Test
	public void testShowArgumentWithBatchName() throws Exception {

		final JobDefinition definition = client.getBatchDefinition(BATCH_NAME);
		assertNotNull(definition);
		assertEquals(BATCH_NAME, definition.getName());
		assertEquals("IT - BatchClient testing job", definition.getDescription());

		final List<Param> params = definition.getParams();
		assertNotNull(params);
		assertEquals(6, params.size());
		assertParam(params.get(0), "dateDebut", "regdate", aujourdhui);
		assertParam(params.get(1), "count", "integer", null);
		assertParam(params.get(2), "duration", "integer", null);
		assertParam(params.get(3), "shutdown_duration", "integer", null);
		assertParam(params.get(4), "salutations", "enum", null);
		assertParam(params.get(5), "attachement", "byte[]", null);

		List<String> values = new ArrayList<String>(3);
		values.add("HELLO");
		values.add("COUCOU");
		values.add("BONJOUR");
		assertEnumParam(params.get(4), "salutations", values, null);
	}

	/**
	 * Vérifie que la commande 'runBatch' ne retourne que lorsque le job est arrêté, même si ce dernier a été interrompu et qu'il prend plusieurs secondes pour s'interrompre.
	 */
	@Test
	public void testRunAndInterruptJob() throws Exception {

		final Map<String, Object> args = new HashMap<String, Object>(2);
		args.put("dateDebut", "2008-03-20");
		args.put("duration", "5"); // 5 secondes
		args.put("shutdown_duration", "20"); // 20 secondes supplémentaires nécessaires en cas d'interruption

		// Démmarre un thread qui va interrompre le job dans 1.0 secondes
		final Thread async = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					client.stopBatch(BATCH_NAME);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		async.start();

		// Démarre le job
		final long start = System.nanoTime();
		client.runBatch(BATCH_NAME, args);
		final long duration = (System.nanoTime() - start) / 1000000000;

		// La méthode runBatch ne doit pas retourner avant que le job soit complétement arrêté (= il doit attendre pendant que le job est interrupting)
		assertTrue(duration > 20L);

		// Finalement, le job doit être dans l'état interrompu
		final JobDefinition definition = client.getBatchDefinition(BATCH_NAME);
		assertNotNull(definition);
		assertEquals(JobStatut.JOB_INTERRUPTED, definition.getStatut());
	}

	private static void assertParam(Param param, final String paramName, final String paramType, final Object paramDefaultValue) {
		assertNotNull(param);
		assertEquals(paramName, param.getName());
		assertEquals(paramType, param.getType());
		assertEquals(paramDefaultValue, param.getDefaultValue());
	}

	private static void assertEnumParam(Param param, final String paramName, final List<String> paramEnumValues, final String paramDefaultValue) {
		assertNotNull(param);
		assertEquals(paramName, param.getName());
		assertEquals(paramEnumValues, param.getEnumValues());
		assertEquals(paramDefaultValue, param.getDefaultValue());
	}
}
