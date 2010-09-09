package ch.vd.uniregctb.editique;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.batch.EditiqueListeRecapJob;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

@SuppressWarnings({"JavaDoc"})
public class EditiqueListeRecapJobTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/editique/ListeRecapJobTest.xml";

	private BatchScheduler batchScheduler;
	private TiersDAO tiersDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test(timeout = 30000)
	@NotTransactional
	/**
	 * Not transactional: pour rendre visibles les données DBUnit au job qui tourne dans un autre thread
	 */
	public void editiqueListeRecapJob() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		HashMap<String, Object> params = new HashMap<String, Object>();
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
		HashMap<String, Object> params1 = new HashMap<String, Object>();
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
		this.doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				checkLRsInTransaction();
				return null;
			}
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
		final Set<Integer> mensuels = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
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
		final Set<Integer> trimestriel = new HashSet<Integer>(Arrays.asList(1, 4, 7, 10));
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
		final Set<Integer> semestriel = new HashSet<Integer>(Arrays.asList(1, 7));
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
