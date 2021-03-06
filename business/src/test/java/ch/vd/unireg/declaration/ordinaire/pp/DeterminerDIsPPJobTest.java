package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.JobTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.utils.UniregModeHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DeterminerDIsPPJobTest extends JobTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminerDIsPPJobTest.class);

	// Copie du fichier tiers-basic sans les données relatives aux déclarations
	private static final String DB_UNIT_DATA_FILE = "DetermineDIsPPJobTest.xml";

	private BatchScheduler batchScheduler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		loadDatabase(DB_UNIT_DATA_FILE);
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {

				MockIndividu lyah = addIndividu(327706, date(1953, 11, 2), "Emery", "Lyah", true);
				addAdresse(lyah, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1980, 1, 1), null);

				MockIndividu pascaline = addIndividu(674417, date(1953, 11, 2), "Decloux", "Pascaline", true);
				addAdresse(pascaline, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), null);

				MockIndividu christine = addIndividu(333905, date(1953, 11, 2), "Schmidt", "Christine", true);
				addAdresse(christine, TypeAdresseCivil.COURRIER, MockRue.LesClees.PlaceDeLaVille, null, date(2001, 6,
						4), null);

				MockIndividu laurent = addIndividu(333908, date(1953, 11, 2), "Schmidt", "Laurent", true);
				addAdresse(laurent, TypeAdresseCivil.COURRIER, MockRue.LesClees.PlaceDeLaVille, null,
						date(2001, 6, 4), null);
			}
		});
	}

	/**
	 * Teste que le job de détermination des DIs en masse fonctionne sans erreur
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsEnMasse() throws Exception {

		UniregModeHelper testMode = getBean(UniregModeHelper.class, "uniregModeHelper");
		testMode.setTestMode("true");//sinon la date de traitement ne sera pas utilisée
		HashMap<String, Object> params = new HashMap<>();
		params.put(DeterminerDIsPPJob.PERIODE_FISCALE, Integer.valueOf(2008));
		params.put(DeterminerDIsPPJob.NB_THREADS, 1);
		params.put(DeterminerDIsPPJob.DATE_TRAITEMENT, date(2009, 1, 16));

		JobDefinition job = batchScheduler.startJob(DeterminerDIsPPJob.NAME, params);

		int count = 0;
		while (job.isRunning()) {
			Thread.sleep(5000);
			count++;
			if (count % 6 == 0) { // 1 minute
				LOGGER.debug("Attente de la fin du job de détermination des DIs");
			}
			if (count > 30) { // 5 minutes
				LOGGER.debug("Interruption du job de détermination des DIs...");
				job.interrupt();
				fail("Le job de détermination des DIs tourne depuis plus de cinq minutes");
			}
		}
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
	}
}
