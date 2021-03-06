package ch.vd.unireg.editique;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.JobTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.editique.batch.EditiqueListeRecapJob;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.PeriodiciteDecompte;

public class EditiqueListeRecapJobTest extends JobTest {

	private static final String DB_UNIT_DATA_FILE = "classpath:ch/vd/unireg/editique/ListeRecapJobTest.xml";

	private BatchScheduler batchScheduler;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test(timeout = 120000)
	/**
	 * Not transactional: pour rendre visibles les données DBUnit au job qui tourne dans un autre thread
	 */
	public void editiqueListeRecapJob() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		HashMap<String, Object> params = new HashMap<>();
		params.put(EditiqueListeRecapJob.S_PARAM_DATE_FIN_PERIODE, date(2008, 12, 25));
		launchJob(params);

		checkLRs();

		// on attend un peu (pour être sûr que le nom du fichier rapport soit différent)
		Thread.sleep(2000);

		//si on relance le batch aucune nouvelle LR ne doit être émise (sinon doublon)
		launchJob(params);
		checkLRs();

		// on attend un peu (pour être sûr que le nom du fichier rapport soit différent)
		Thread.sleep(2000);

		//si on lance le batch pour une date à laquelle les DPI n'ont pas de for, aucune LR ne doit être créée
		HashMap<String, Object> params1 = new HashMap<>();
		params1.put(EditiqueListeRecapJob.S_PARAM_DATE_FIN_PERIODE, date(2007, 12, 31));
		launchJob(params1);
		checkLRs();
	}

	public void launchJob(HashMap<String, Object> params) throws Exception {

		final Date statTime = DateHelper.getCurrentDate();
		final JobDefinition job = batchScheduler.startJob(EditiqueListeRecapJob.NAME, params);

		// Attente du démarrage de l'exécution
		waitUntilRunning(job, statTime);

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(2000);
			logger.debug("Attente de la fin du job de creation des LRs");
		}
		Assert.assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
	}

	/**
	 * Execution en transaction: pour permettre l'initialisation LAZY des collections
	 */
	private void checkLRs() throws Exception {
		this.doInNewTransaction(status -> {
			checkLRsInTransaction();
			return null;
		});
	}

	/**
	 * Verification que les LRs sont creees dans la base
	 */
	// FIXME(FDE) : Aussi checker que les evt fiscaux sont envoyés
	private void checkLRsInTransaction() {
		// Debiteur 12500001
		Tiers tiers = tiersDAO.get((long) 12500001);
		Assert.assertNotNull(tiers.getDeclarations());
		Set<Declaration> declarations = tiers.getDeclarations();
		Assert.assertEquals(12, declarations.size());		// toute l'année
		final Set<Integer> mensuels = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
		for (Declaration decl : declarations) {
			final DeclarationImpotSource lr = (DeclarationImpotSource) decl;
			Assert.assertEquals(PeriodiciteDecompte.MENSUEL, lr.getPeriodicite());

			// quel mois?
			Assert.assertEquals(lr.getDateDebut().month(), lr.getDateFin().month());
			final int mois = lr.getDateDebut().month();
			Assert.assertTrue(mensuels.contains(mois));
			mensuels.remove(mois);

			Assert.assertEquals(2008, lr.getDateDebut().year());
			Assert.assertEquals(2008, lr.getDateFin().year());
			Assert.assertEquals(1, lr.getDateDebut().day());
			Assert.assertEquals(lr.getDateFin().getLastDayOfTheMonth(), lr.getDateFin());

			Assert.assertNotNull(lr.getId());
		}
		Assert.assertTrue(mensuels.isEmpty());

		// Debiteur 12500002
		tiers = tiersDAO.get((long) 12500002);
		Assert.assertNotNull(tiers.getDeclarations());
		declarations = tiers.getDeclarations();
		Assert.assertEquals(4, declarations.size());		// toute l'année
		final Set<Integer> trimestriel = new HashSet<>(Arrays.asList(1, 4, 7, 10));
		for (Declaration decl : declarations) {
			final DeclarationImpotSource lr = (DeclarationImpotSource) decl;
			Assert.assertEquals(PeriodiciteDecompte.TRIMESTRIEL, lr.getPeriodicite());

			// quel trimestre?
			Assert.assertEquals(lr.getDateDebut().month() + 2, lr.getDateFin().month());
			Assert.assertEquals(1, lr.getDateDebut().day());
			Assert.assertEquals(lr.getDateFin().getLastDayOfTheMonth(), lr.getDateFin());
			Assert.assertEquals(2008, lr.getDateDebut().year());
			Assert.assertEquals(2008, lr.getDateFin().year());

			final int mois = lr.getDateDebut().month();
			Assert.assertTrue(trimestriel.contains(mois));
			trimestriel.remove(mois);

			Assert.assertNotNull(lr.getId());
		}
		Assert.assertTrue(trimestriel.isEmpty());

		// Debiteur 12500003
		tiers = tiersDAO.get((long) 12500003);
		Assert.assertNotNull(tiers.getDeclarations());
		declarations = tiers.getDeclarations();
		Assert.assertEquals(2, declarations.size());		// toute l'année
		final Set<Integer> semestriel = new HashSet<>(Arrays.asList(1, 7));
		for (Declaration decl : declarations) {
			final DeclarationImpotSource lr = (DeclarationImpotSource) decl;
			Assert.assertEquals(PeriodiciteDecompte.SEMESTRIEL, lr.getPeriodicite());

			// quel semestre?
			Assert.assertEquals(lr.getDateDebut().month() + 5, lr.getDateFin().month());
			Assert.assertEquals(1, lr.getDateDebut().day());
			Assert.assertEquals(lr.getDateFin().getLastDayOfTheMonth(), lr.getDateFin());
			Assert.assertEquals(2008, lr.getDateDebut().year());
			Assert.assertEquals(2008, lr.getDateFin().year());

			final int mois = lr.getDateDebut().month();
			Assert.assertTrue(semestriel.contains(mois));
			semestriel.remove(mois);

			Assert.assertNotNull(lr.getId());
		}
		Assert.assertTrue(semestriel.isEmpty());

		// Debiteur 12500004
		tiers = tiersDAO.get((long) 12500004);
		Assert.assertNotNull(tiers.getDeclarations());
		declarations = tiers.getDeclarations();
		Assert.assertEquals(1, declarations.size());
		{
			final DeclarationImpotSource lr = (DeclarationImpotSource) declarations.iterator().next();
			Assert.assertEquals(PeriodiciteDecompte.ANNUEL, lr.getPeriodicite());
			Assert.assertEquals(RegDate.get(2008, 1, 1), lr.getDateDebut());
			Assert.assertEquals(RegDate.get(2008, 12, 31), lr.getDateFin());
			Assert.assertNotNull(lr.getId());
		}
	}

}
