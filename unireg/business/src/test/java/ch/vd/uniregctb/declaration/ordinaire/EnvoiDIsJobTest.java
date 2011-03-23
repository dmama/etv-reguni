package ch.vd.uniregctb.declaration.ordinaire;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobTest;
import ch.vd.uniregctb.declaration.EnvoiDIsJob;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class EnvoiDIsJobTest extends JobTest {

	private final Logger LOGGER = Logger.getLogger(EnvoiDIsJobTest.class);

	// Copie du fichier tiers-basic sans les données relatives aux déclarations
	private final static String DB_UNIT_DATA_FILE = "EnvoiDIsJobTest.xml";

	private BatchScheduler batchScheduler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		loadDatabase(DB_UNIT_DATA_FILE);
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
	}

	/**
	 * Teste que le job d'envoi des DIs en masse fonctionne sans erreur
	 */
	@Test
	public void testEnvoiDIsEnMasse() throws Exception {

		final Map<String, Object> params = new HashMap<String, Object>();
		params.put(EnvoiDIsJob.PERIODE_FISCALE, RegDate.get().year() -1);
		params.put(EnvoiDIsJob.CATEGORIE_CTB, CategorieEnvoiDI.VAUDOIS_COMPLETE);
		params.put(EnvoiDIsJob.EXCLURE_DCD, Boolean.FALSE);
		final JobDefinition job = batchScheduler.startJob(EnvoiDIsJob.NAME, params);

		int count = 0;
		while (job.isRunning()) {
			Thread.sleep(5000);
			count++;
			if (count % 6 == 0) { // 1 minute
				LOGGER.debug("Attente de la fin du job d'envoi des DIs");
			}
			if (count > 30) { // 5 minutes
				LOGGER.debug("Interruption du job d'envoi des DIs...");
				job.interrupt();
				fail("Le job d'envoi des DIs tourne depuis plus de cinq minutes");
			}
		}
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
	}
}
