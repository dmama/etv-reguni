package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

public class ImportCodesSegmentProcessorTest extends BusinessTest {

	private ImportCodesSegmentProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		processor = new ImportCodesSegmentProcessor(hibernateTemplate, transactionManager, tiersService, adresseService);
	}

	@Test
	public void testEmptyInput() throws Exception {
		final ImportCodesSegmentResults res = processor.run(Collections.<ContribuableAvecCodeSegment>emptyList(), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(0, res.getErreurs().size());
	}

	@Test
	public void testCtbInconnu() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final long noCtb = 10020040L;
		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noCtb, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(1, res.getErreurs().size());

		final ImportCodesSegmentResults.Erreur erreur = res.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ImportCodesSegmentResults.ErreurType.CTB_INCONNU, erreur.type);
		Assert.assertEquals(noCtb, erreur.noTiers);
	}


	@Test
	public void testCodeSegmentInvalide() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final long noCtb = 10020040L;
		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noCtb, 23));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(1, res.getErreurs().size());

		final ImportCodesSegmentResults.Erreur erreur = res.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ImportCodesSegmentResults.ErreurType.CODE_SEGMENT_INVALIDE, erreur.type);
		Assert.assertEquals("23", erreur.details);
		Assert.assertEquals(noCtb, erreur.noTiers);
	}

	@Test
	public void testTiersNonContribuable() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			return dpi.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(1, res.getErreurs().size());

		final ImportCodesSegmentResults.Erreur erreur = res.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ImportCodesSegmentResults.ErreurType.TIERS_PAS_CONTRIBUABLE, erreur.type);
		Assert.assertEquals(NatureTiers.DebiteurPrestationImposable.name(), erreur.details);
		Assert.assertEquals(noTiers, erreur.noTiers);
	}

	@Test
	public void testContribuableSansDeclaration() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(1, res.getErreurs().size());

		final ImportCodesSegmentResults.Erreur erreur = res.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ImportCodesSegmentResults.ErreurType.CTB_SANS_DECLARATION, erreur.type);
		Assert.assertEquals(noTiers, erreur.noTiers);
	}

	@Test
	public void testContribuableDejaBonSegment() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			final PeriodeFiscale pf = addPeriodeFiscale(2009);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
			di.setCodeSegment(4);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(0, res.getErreurs().size());

		final ImportCodesSegmentResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(noTiers, ignore.noTiers);
		Assert.assertEquals("Le contribuable est déjà assigné au bon segment", ignore.cause);

		// on vérifie tout de même que le code segment est toujours le bon
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
			final DeclarationImpotOrdinairePP di = pp.getDerniereDeclaration(DeclarationImpotOrdinairePP.class);
			Assert.assertEquals(4, (int) di.getCodeSegment());
			return null;
		});
	}

	@Test
	public void testContribuablePremiereFoisSegment() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			final PeriodeFiscale pf = addPeriodeFiscale(2009);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
			di.setCodeSegment(null);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(1, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(0, res.getErreurs().size());

		final ImportCodesSegmentResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(noTiers, traite.noTiers);
		Assert.assertEquals(4, traite.codeSegment);

		// on vérifie tout de même que le travail a aussi été fait en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
			final DeclarationImpotOrdinairePP di = pp.getDerniereDeclaration(DeclarationImpotOrdinairePP.class);
			Assert.assertEquals(4, (int) di.getCodeSegment());
			return null;
		});
	}

	@Test
	public void testContribuableChangementSegment() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			final PeriodeFiscale pf = addPeriodeFiscale(2009);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
			di.setCodeSegment(2);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(1, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(0, res.getErreurs().size());

		final ImportCodesSegmentResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(noTiers, traite.noTiers);
		Assert.assertEquals(4, traite.codeSegment);

		// on vérifie tout de même que le travail a aussi été fait en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
			final DeclarationImpotOrdinairePP di = pp.getDerniereDeclaration(DeclarationImpotOrdinairePP.class);
			Assert.assertEquals(4, (int) di.getCodeSegment());
			return null;
		});
	}

	@Test
	public void testContribuableToutesDeclarationsAnnulees() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			final PeriodeFiscale pf = addPeriodeFiscale(2009);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
			di.setAnnule(true);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(0, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(1, res.getErreurs().size());

		final ImportCodesSegmentResults.Erreur erreur = res.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ImportCodesSegmentResults.ErreurType.CTB_SANS_DECLARATION, erreur.type);
		Assert.assertEquals(noTiers, erreur.noTiers);
	}

	@Test
	public void testContribuableAvecDeclarationAnnuleeEtAutreNonAnnulee() throws Exception {

		// service civil vide
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// préparation fiscale
		final long noTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
			final PeriodeFiscale pf = addPeriodeFiscale(2009);
			final ModeleDocument mdHc = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
			final ModeleDocument mdDepense = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, pf);

			final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, mdHc);
			diAnnulee.setAnnule(true);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 10, 31), TypeContribuable.VAUDOIS_DEPENSE, mdDepense);
			di.setCodeSegment(3);
			return pp.getNumero();
		});

		final List<ContribuableAvecCodeSegment> coll = Collections.singletonList(new ContribuableAvecCodeSegment(noTiers, 4));
		final ImportCodesSegmentResults res = processor.run(coll, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNombreTiersAnalyses());
		Assert.assertNotNull(res.getTraites());
		Assert.assertEquals(1, res.getTraites().size());
		Assert.assertNotNull(res.getIgnores());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertNotNull(res.getErreurs());
		Assert.assertEquals(0, res.getErreurs().size());

		final ImportCodesSegmentResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(noTiers, traite.noTiers);
		Assert.assertEquals(4, traite.codeSegment);

		// on vérifie tout de même que le travail a aussi été fait en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
			final DeclarationImpotOrdinairePP di = pp.getDerniereDeclaration(DeclarationImpotOrdinairePP.class);
			Assert.assertEquals(date(2009, 10, 31), di.getDateFin());
			Assert.assertEquals(4, (int) di.getCodeSegment());
			return null;
		});
	}
}
