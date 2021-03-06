package ch.vd.unireg.declaration.ordinaire.pp;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

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
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final TicketService ticketService = getBean(TicketService.class, "ticketService");

		processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDocumentDAO, tacheDAO, tiersService, delaisService, diService, transactionManager,
		                                                   parametreAppService, serviceCivilCacheWarmer, validationService,
		                                                   periodeImpositionService, adresseService, ticketService, audit);
	}

	@Test
	public void testDIEmise() throws Exception {

		final int year = RegDate.get().year();

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1967, 5, 12), Sexe.MASCULIN);
			addForPrincipal(pp, date(year - 2, 4, 12), MotifFor.ARRIVEE_HS, date(year - 1, 10, 23), MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);

			final PeriodeFiscale pf1 = addPeriodeFiscale(year - 2);
			final PeriodeFiscale pf2 = addPeriodeFiscale(year - 1);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2);
			final ModeleDocument md1 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf1);
			final ModeleDocument md2 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2);
			addDeclarationImpot(pp, pf1, date(year - 2, 4, 12), date(year - 2, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md1);
			addDeclarationImpot(pp, pf2, date(year - 1, 1, 1), date(year - 1, 10, 23), TypeContribuable.VAUDOIS_ORDINAIRE, md2);
			return null;
		});

		{
			final ListeDIsPPNonEmises results = processor.run(year - 2, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsPPNonEmises results = processor.run(year - 1, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsPPNonEmises results = processor.run(year, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(0, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
	}

	@Test
	public void testDInonEmise() throws Exception {

		final int year = RegDate.get().year();

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1967, 5, 12), Sexe.MASCULIN);
			addForPrincipal(pp, date(year - 2, 4, 12), MotifFor.ARRIVEE_HS, date(year - 1, 10, 23), MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);

			final PeriodeFiscale pf1 = addPeriodeFiscale(year - 2);
			final PeriodeFiscale pf2 = addPeriodeFiscale(year - 1);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf1);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf2);
			return null;
		});

		{
			final ListeDIsPPNonEmises results = processor.run(year - 2, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(1, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(1, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsPPNonEmises results = processor.run(year - 1, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(1, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(1, results.nbCtbsTotal);
			Assert.assertEquals(1, results.getNombreDeDIsNonEmises());
		}
		{
			final ListeDIsPPNonEmises results = processor.run(year, RegDate.get(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.ctbsEnErrors.size());
			Assert.assertEquals(0, results.ctbsIgnores.size());
			Assert.assertEquals(0, results.ctbsIndigents.size());
			Assert.assertEquals(0, results.ctbsAvecDiGeneree.size());
			Assert.assertEquals(0, results.nbCtbsTotal);
			Assert.assertEquals(0, results.getNombreDeDIsNonEmises());
		}
	}
}
