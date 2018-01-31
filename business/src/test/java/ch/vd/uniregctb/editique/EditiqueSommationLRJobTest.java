package ch.vd.uniregctb.editique;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.JobTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.batch.EditiqueSommationLRJob;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class EditiqueSommationLRJobTest extends JobTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditiqueSommationLRJobTest.class);

	private static final String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/editique/SommationLRTest.xml";

	private BatchScheduler batchScheduler;
	private TiersDAO tiersDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test(timeout = 30000)
	@Transactional(rollbackFor = Throwable.class)
	public void testEditiqueSommationLRJob() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		final Date statTime = DateHelper.getCurrentDate();
		final JobDefinition job = batchScheduler.startJob(EditiqueSommationLRJob.NAME, null);
		
		// Attente du démarrage de l'exécution
		waitUntilRunning(job, statTime);

		// Attente de l'arrêt de l'exécution
		while (job.isRunning()) {
			Thread.sleep(2000);
			LOGGER.debug("Attente de la fin du job de creation de sommation des LRs");
		}
		Assert.assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		//Verification que la LR est passe a l'etat SOMME
		//Debiteur 12500001 avec delais
		Tiers tiers = tiersDAO.get((long) 12500001);
		Assert.assertNotNull(tiers.getDeclarations());
		Set<Declaration> declarations = tiers.getDeclarations();
		Iterator<Declaration> itDec = declarations.iterator();
		DeclarationImpotSource lr = (DeclarationImpotSource) itDec.next();
		Assert.assertEquals(TypeEtatDocumentFiscal.SOMME, lr.getDernierEtatDeclaration().getEtat());

		//Verification que la LR n'a pas été SOMME
		//Debiteur 12500002 avec delai en 2020
		tiers = tiersDAO.get((long) 12500002);
		Assert.assertNotNull(tiers.getDeclarations());
		declarations = tiers.getDeclarations();
		itDec = declarations.iterator();
		lr = (DeclarationImpotSource) itDec.next();
		Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lr.getDernierEtatDeclaration().getEtat());
	}

}
