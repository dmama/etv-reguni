package ch.vd.uniregctb.declaration.ordinaire;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.ValidationService;

public class ProduireListeDIsNonEmisesProcessorTest extends BusinessTest {

	private ProduireListeDIsNonEmisesProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDocumentDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final TacheDAO tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		final ParametreAppService parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");

		processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDocumentDAO, tacheDAO, tiersService, delaisService, diService, transactionManager, parametreAppService, serviceCivilCacheWarmer, validationService);
	}

	@Test
	public void testDIEmise() throws Exception {

		final int year = RegDate.get().year();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1967, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year - 2, 4, 12), MotifFor.ARRIVEE_HS, date(year - 1, 10, 23), MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);

				final PeriodeFiscale pf1 = addPeriodeFiscale(year - 2);
				final PeriodeFiscale pf2 = addPeriodeFiscale(year - 1);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2);
				final ModeleDocument md1 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf1);
				final ModeleDocument md2 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				addDeclarationImpot(pp, pf1, date(year - 2, 4, 12), date(year - 2, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md1);
				addDeclarationImpot(pp, pf2, date(year - 1, 1, 1), date(year - 1, 10, 23), TypeContribuable.VAUDOIS_ORDINAIRE, md2);
				return null;
			}
		});

		{
			final ListeDIsNonEmises results = processor.run(year - 2, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(0, results.ctbsTraites.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsNonEmises results = processor.run(year - 1, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(0, results.ctbsTraites.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsNonEmises results = processor.run(year, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(0, results.ctbsTraites.size());
			Assert.assertEquals(0, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
	}

	@Test
	public void testDInonEmise() throws Exception {

		final int year = RegDate.get().year();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1967, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year - 2, 4, 12), MotifFor.ARRIVEE_HS, date(year - 1, 10, 23), MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);

				final PeriodeFiscale pf1 = addPeriodeFiscale(year - 2);
				final PeriodeFiscale pf2 = addPeriodeFiscale(year - 1);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				return null;
			}
		});

		{
			final ListeDIsNonEmises results = processor.run(year - 2, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(1, results.ctbsTraites.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(1, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsNonEmises results = processor.run(year - 1, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(1, results.ctbsTraites.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(1, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsNonEmises results = processor.run(year, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsRollback.size());
			Assert.assertEquals(0, results.ctbsTraites.size());
			Assert.assertEquals(0, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
	}
}
