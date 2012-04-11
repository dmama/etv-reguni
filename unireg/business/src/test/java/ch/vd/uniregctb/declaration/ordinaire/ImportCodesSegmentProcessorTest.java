package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class ImportCodesSegmentProcessorTest extends BusinessTest {

	private ImportCodesSegmentProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		processor = new ImportCodesSegmentProcessor(hibernateTemplate, transactionManager, tiersService);
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
		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noCtb, 4));
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
		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noCtb, 23));
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				return dpi.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setCodeSegment(4);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) pp.getDerniereDeclaration();
				Assert.assertEquals(4, (int) di.getCodeSegment());
				return null;
			}
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setCodeSegment(null);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) pp.getDerniereDeclaration();
				Assert.assertEquals(4, (int) di.getCodeSegment());
				return null;
			}
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setCodeSegment(2);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) pp.getDerniereDeclaration();
				Assert.assertEquals(4, (int) di.getCodeSegment());
				return null;
			}
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, md);
				di.setAnnule(true);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		final long noTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", null, Sexe.FEMININ);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument mdHc = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final ModeleDocument mdDepense = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, pf);

				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, mdHc);
				diAnnulee.setAnnule(true);

				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 10, 31), TypeContribuable.VAUDOIS_DEPENSE, mdDepense);
				di.setCodeSegment(3);
				return pp.getNumero();
			}
		});

		final List<ContribuableAvecCodeSegment> coll = Arrays.asList(new ContribuableAvecCodeSegment(noTiers, 4));
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
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noTiers);
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) pp.getDerniereDeclaration();
				Assert.assertEquals(date(2009, 10, 31), di.getDateFin());
				Assert.assertEquals(4, (int) di.getCodeSegment());
				return null;
			}
		});
	}
}
