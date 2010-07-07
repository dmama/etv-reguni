package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.uniregctb.utils.UniregModeHelper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeterminerDIsJob;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class DetermineDIsJobTest extends BusinessTest {

	private final Logger LOGGER = Logger.getLogger(DetermineDIsJobTest.class);

	// Copie du fichier tiers-basic sans les données relatives aux déclarations
	private final static String DB_UNIT_DATA_FILE = "DetermineDIsJobTest.xml";

	private BatchScheduler batchScheduler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		loadDatabase(DB_UNIT_DATA_FILE);
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				MockIndividu lyah = addIndividu(327706, date(1953, 11, 2), "Emery", "Lyah", true);
				addAdresse(lyah, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1980, 1, 1), null);

				MockIndividu pascaline = addIndividu(674417, date(1953, 11, 2), "Decloux", "Pascaline", true);
				addAdresse(pascaline, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), null);

				MockIndividu christine = addIndividu(333905, date(1953, 11, 2), "Schmidt", "Christine", true);
				addAdresse(christine, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null, date(2001, 6,
						4), null);

				MockIndividu laurent = addIndividu(333908, date(1953, 11, 2), "Schmidt", "Laurent", true);
				addAdresse(laurent, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
						date(2001, 6, 4), null);
			}
		});

		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				for (MockCollectiviteAdministrative ca : MockCollectiviteAdministrative.getAll()) {
					addCollAdm(ca);
				}
				return null;
			}
		});
	}

	/**
	 * Teste que le job de détermination des DIs en masse fonctionne sans erreur
	 */
	@Test
	public void testDetermineDIsEnMasse() throws Exception {

		UniregModeHelper testMode = getBean(UniregModeHelper.class, "uniregModeHelper");
		testMode.setTestMode("true");//sinon la date de traitement ne sera pas utilisée
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(DeterminerDIsJob.PERIODE_FISCALE, Integer.valueOf(2008));
		params.put(DeterminerDIsJob.NB_THREADS, 1);
		params.put(DeterminerDIsJob.DATE_TRAITEMENT, date(2009, 1, 16));

		JobDefinition job = batchScheduler.startJob(DeterminerDIsJob.NAME, params);

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
