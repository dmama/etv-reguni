package ch.vd.uniregctb.editique;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.batch.EditiqueSommationLRJob;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EditiqueSommationLRJobTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(EditiqueSommationLRJobTest.class);

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/editique/SommationLRTest.xml";

	private BatchScheduler batchScheduler;
	private TiersDAO tiersDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testEditiqueSommationLRJob() throws Exception {

		LOGGER.debug("EditiqueListeRecapJobTest - testEditiqueSommationLRJob");

		loadDatabase(DB_UNIT_DATA_FILE);

		JobDefinition job = batchScheduler.startJobWithDefaultParams(EditiqueSommationLRJob.NAME);
		while (job.isRunning()) {
			Thread.sleep(2000);
			LOGGER.debug("Attente de la fin du job de creation de sommation des LRs");
		}
		Assert.assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		//Verification que la LR est passe a l'etat SOMME
		//Debiteur 12500001 avec delais
		Tiers tiers = tiersDAO.get(new Long(12500001));
		Assert.assertNotNull(tiers.getDeclarations());
		Set<Declaration> declarations = tiers.getDeclarations();
		Iterator<Declaration> itDec = declarations.iterator();
		DeclarationImpotSource lr = (DeclarationImpotSource) itDec.next();
		Assert.assertEquals(TypeEtatDeclaration.SOMMEE, lr.getDernierEtat().getEtat());

		//Verification que la LR n'a pas été SOMME
		//Debiteur 12500002 avec delai en 2020
		tiers = tiersDAO.get(new Long(12500002));
		Assert.assertNotNull(tiers.getDeclarations());
		declarations = tiers.getDeclarations();
		itDec = declarations.iterator();
		lr = (DeclarationImpotSource) itDec.next();
		Assert.assertEquals(TypeEtatDeclaration.EMISE, lr.getDernierEtat().getEtat());

	}

}
