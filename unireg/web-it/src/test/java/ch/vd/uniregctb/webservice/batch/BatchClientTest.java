package ch.vd.uniregctb.webservice.batch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.ubr.BatchRunnerClient;
import ch.vd.uniregctb.ubr.BatchRunnerClientException;
import ch.vd.uniregctb.ubr.JobDescription;
import ch.vd.uniregctb.ubr.JobParamDescription;
import ch.vd.uniregctb.ubr.JobStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class BatchClientTest extends WebitTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchClientTest.class);

	private static final String BATCH_NAME = "IT-BatchClientJob";

	private BatchRunnerClient client;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		LOGGER.info("Connecting to: " + batchUrl + " with user = " + username);
		client = new BatchRunnerClient(batchUrl, username, password);
	}

	@Test
	public void testGetListJobs() throws Exception {

		final List<String> list = client.getBatchNames();
		assertNotNull(list);
		assertEquals(96, list.size());
		int i = 0;
		assertEquals("IT-BatchClientJob", list.get(i++));
		assertEquals("IT-InContainerTestingJob", list.get(i++));
		assertEquals("DumpAssujettissementsJob", list.get(i++));
		assertEquals("DatabaseIndexerJob", list.get(i++));
		assertEquals("OptimizeIndexJob", list.get(i++));
		assertEquals("CheckCoherenceIndexerJob", list.get(i++));
		assertEquals("MessageIdentificationIndexerJob", list.get(i++));
		assertEquals("OfficeImpotIndexerJob", list.get(i++));
		assertEquals("SuppressionOIDJob", list.get(i++));
		assertEquals("RapprocherCtbRegistreFoncierJob", list.get(i++));
		assertEquals("ImportImmeublesJob", list.get(i++));
		assertEquals("TraiterImportRFJob", list.get(i++));
		assertEquals("TraiterMutationsRFJob", list.get(i++));
		assertEquals("RapprocherTiersRFJob", list.get(i++));
		assertEquals("TraiterFinsDeDroitsRFJob", list.get(i++));
		assertEquals("CleanupImportRFJob", list.get(i++));
		assertEquals("DeterminerMouvementsDossiersEnMasseJob", list.get(i++));
		assertEquals("ResolutionAdresseJob", list.get(i++));
		assertEquals("ComparerSituationFamilleJob", list.get(i++));
		assertEquals("AnnonceIDEJob", list.get(i++));
		assertEquals("RattrapageRegimesFiscauxJob", list.get(i++));
		assertEquals("UpdateTacheStatsJob", list.get(i++));
		assertEquals("RecalculTachesJob", list.get(i++));
		assertEquals("UpdateCriteresIdentificationJob", list.get(i++));
		assertEquals("IdentifierContribuableFromListeJob", list.get(i++));
		assertEquals("EvenementCivilHandlerJob", list.get(i++));
		assertEquals("IdentifierContribuableJob", list.get(i++));
		assertEquals("EvenementExterneHandlerJob", list.get(i++));
//		assertEquals("EvenementCivilEchCorrectionDumpJob", list.get(i++));      // pas la peine de le mettre, il n'est visible qu'en dev...
		assertEquals("EvenementReqDesHandlerJob", list.get(i++));
		assertEquals("EvenementOrganisationHandlerJob", list.get(i++));
		assertEquals("EditiqueListeRecapJob", list.get(i++));
		assertEquals("EditiqueSommationLRJob", list.get(i++));
		assertEquals("ReinitialiserBaremeDoubleGainJob", list.get(i++));
		assertEquals("DeterminerLRsEchuesJob", list.get(i++));
		assertEquals("DetermineDIsEnMasseJob", list.get(i++));
		assertEquals("EnvoiDIsEnMasseJob", list.get(i++));
		assertEquals("EditiqueSommationDIJob", list.get(i++));
		assertEquals("EchoirDIsJob", list.get(i++));
		assertEquals("ExclureContribuablesEnvoiJob", list.get(i++));
		assertEquals("DemandeDelaiCollectiveJob", list.get(i++));
		assertEquals("ListeNoteJob", list.get(i++));
		assertEquals("EnvoiAnnexeImmeubleJob", list.get(i++));
		assertEquals("ImportCodesSegmentJob", list.get(i++));
		assertEquals("DetermineDIsPMEnMasseJob", list.get(i++));
		assertEquals("EnvoiDIsPMEnMasseJob", list.get(i++));
		assertEquals("EnvoiSommationsDIsPMJob", list.get(i++));
		assertEquals("EchoirDIsPMJob", list.get(i++));
		assertEquals("EnvoiDemandesDegrevementICIJob", list.get(i++));
		assertEquals("RappelDemandesDegrevementICIJob", list.get(i++));
		assertEquals("DeterminerQuestionnairesSNCJob", list.get(i++));
		assertEquals("EnvoiQuestionnairesSNCEnMasseJob", list.get(i++));
		assertEquals("EnvoiRappelsQuestionnairesSNCJob", list.get(i++));
		assertEquals("EnvoiLettresBienvenueJob", list.get(i++));
		assertEquals("RappelLettresBienvenueJob", list.get(i++));
		assertEquals("OuvertureForsContribuableMajeurJob", list.get(i++));
		assertEquals("FusionDeCommunesJob", list.get(i++));
		assertEquals("ComparerForFiscalEtCommuneJob", list.get(i++));
		assertEquals("PassageNouveauxRentiersSourciersEnMixteJob", list.get(i++));
		assertEquals("RolePPCommunesJob", list.get(i++));
		assertEquals("RolePPOfficesJob", list.get(i++));
		assertEquals("RolePMCommunesJob", list.get(i++));
		assertEquals("RolePMOfficeJob", list.get(i++));
		assertEquals("ProduireRolesPPCommuneJob", list.get(i++));
		assertEquals("ProduireRolesOIDJob", list.get(i++));
		assertEquals("ProduireRolesPMCommuneJob", list.get(i++));
		assertEquals("ProduireRolesOIPMJob", list.get(i++));
		assertEquals("ProduireStatsJob", list.get(i++));
		assertEquals("ListeDINonEmisesJob", list.get(i++));
		assertEquals("ListesNominativesJob", list.get(i++));
		assertEquals("AcomptesJob", list.get(i++));
		assertEquals("ExtractionDonneesRptJob", list.get(i++));
		assertEquals("ListeAssujettisJob", list.get(i++));
		assertEquals("ValidationJob", list.get(i++));
		assertEquals("ListeTachesEnInstanceParOIDJob", list.get(i++));
		assertEquals("ListeCtbsResidentsSansForVdJob", list.get(i++));
		assertEquals("StatistiquesEvenementsJob", list.get(i++));
		assertEquals("ListeDroitsAccesJob", list.get(i++));
		assertEquals("DumpPeriodesImpositionImpotSourceJob", list.get(i++));
		assertEquals("ListeAssujettisParSubstitutionJob", list.get(i++));
		assertEquals("InitialisationIFoncJob", list.get(i++));
		assertEquals("ExtractionRegimesFiscauxJob", list.get(i++));
		assertEquals("DumpDatabaseJob", list.get(i++));
		assertEquals("LoadDatabaseJob", list.get(i++));
		assertEquals("RamasseDocumentJob", list.get(i++));
		assertEquals("UpdateSequencesJob", list.get(i++));
		assertEquals("DumpTiersListJob", list.get(i++));
		assertEquals("CorrectionForsHCJob", list.get(i++));
		assertEquals("CorrectionFlagHabitantJob", list.get(i++));
		assertEquals("CorrectionEtatDeclarationJob", list.get(i++));
		assertEquals("InsertEmployeurFictifEmpAciJob", list.get(i++));
		assertEquals("AuditLogPurgeJob", list.get(i++));
		assertEquals("CalculParentesJob", list.get(i++));
		assertEquals("AppariementEtablissementsSecondairesJob", list.get(i++));
		assertEquals("MigrationDDJob", list.get(i++));
		assertEquals("MigrationExoIFONCJob", list.get(i++));
		assertEquals("CacheResetJob", list.get(i++));
	}

	@Test
	public void testStartJobWithNoBatchName() throws Exception {

		try {
			client.startBatch("", null);
			fail("il ne devrait pas être possible de pouvoir démarrer un batch nul");
		}
		catch (BatchRunnerClientException e) {
			assertEquals("HTTP error code 404 received from the server", e.getMessage());
		}
	}

	@Test
	public void testStartJobWithBadBatchName() throws Exception {

		try {
			client.startBatch("inconnu", null);
			fail("il ne devrait pas être possible de pouvoir démarrer un batch inconnu");
		}
		catch (BatchRunnerClientException e) {
			assertEquals("HTTP error code 404 received from the server", e.getMessage());
			assertNotNull(e.getCause());
			assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("Job 'inconnu' not found"));
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

		final Map<String, Object> args = new HashMap<>(2);
		args.put("dateDebut", "2008-03-20");
		args.put("count", "12");
		client.startBatch(BATCH_NAME, args);

		// on attend maintenant la fin du job avant de continuer
		client.stopBatch(BATCH_NAME);
	}

	@Test
	public void testStopJobWithBatchName() throws Exception {

		// démarre le job avec le paramètre duration = 10 secondes, de telle manière qui'il dure assez longtemps pour qu'on puisse l'arrêter
		Map<String, Object> args = new HashMap<>(1);
		args.put("duration", "10");
		client.startBatch(BATCH_NAME, args);

		// on attend maintenant la fin du job avant de continuer
		client.stopBatch(BATCH_NAME);

		// le job doit être dans l'état interrompu
		final JobDescription description = client.getBatchDescription(BATCH_NAME);
		assertNotNull(description);
		assertEquals(JobStatus.INTERRUPTED, description.getStatus());
	}

	@Test
	public void testShowArgumentWithBatchName() throws Exception {

		final JobDescription description = client.getBatchDescription(BATCH_NAME);
		assertNotNull(description);
		assertEquals(BATCH_NAME, description.getName());
		assertEquals("IT - BatchClient testing job", description.getDescription());

		final List<JobParamDescription> params = description.getParameters();
		assertNotNull(params);
		assertEquals(6, params.size());
		assertParam(params.get(0), "dateDebut", "regdate");
		assertParam(params.get(1), "count", "integer");
		assertParam(params.get(2), "duration", "integer");
		assertParam(params.get(3), "shutdown_duration", "integer");
		assertParam(params.get(4), "salutations", "enum");
		assertParam(params.get(5), "attachement", "byte[]");

		List<String> values = new ArrayList<>(3);
		values.add("HELLO");
		values.add("COUCOU");
		values.add("BONJOUR");
		assertEnumParam(params.get(4), "salutations", values);
	}

	/**
	 * Vérifie que la commande 'runBatch' ne retourne que lorsque le job est arrêté, même si ce dernier a été interrompu et qu'il prend plusieurs secondes pour s'interrompre.
	 */
	@Test
	public void testRunAndInterruptJob() throws Exception {

		final Map<String, Object> args = new HashMap<>(2);
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
		final JobDescription description = client.getBatchDescription(BATCH_NAME);
		assertNotNull(description);
		assertEquals(JobStatus.INTERRUPTED, description.getStatus());
	}

	private static void assertParam(JobParamDescription param, final String paramName, final String paramType) {
		assertNotNull(param);
		assertEquals(paramName, param.getName());
		assertEquals(paramType, param.getType());
	}

	private static void assertEnumParam(JobParamDescription param, final String paramName, final List<String> paramEnumValues) {
		assertNotNull(param);
		assertEquals(paramName, param.getName());
		assertEquals(paramEnumValues, Arrays.asList(param.getEnumValues()));
	}
}
