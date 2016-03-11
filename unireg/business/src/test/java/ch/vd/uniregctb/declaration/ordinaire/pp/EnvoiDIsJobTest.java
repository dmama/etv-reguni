package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobTest;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EnvoiDIsJobTest extends JobTest {

	private final Logger LOGGER = LoggerFactory.getLogger(EnvoiDIsJobTest.class);

	// Copie du fichier tiers-basic sans les données relatives aux déclarations
	private static final String DB_UNIT_DATA_FILE = "EnvoiDIsPPJobTest.xml";

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
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIsEnMasse() throws Exception {

		final Map<String, Object> params = new HashMap<>();
		params.put(EnvoiDIsJob.PERIODE_FISCALE, RegDate.get().year() -1);
		params.put(EnvoiDIsJob.CATEGORIE_CTB, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
		params.put(EnvoiDIsJob.EXCLURE_DCD, Boolean.FALSE);
		params.put(EnvoiDIsJob.NB_THREADS, 4);
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
