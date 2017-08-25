package ch.vd.uniregctb.declaration.source;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiSommationLRsEnMasseProcessorTest extends BusinessTest {

	private ListeRecapService lrService;
	private DelaisService delaisService;
	private ListeRecapitulativeDAO lrDAO;

	private EnvoiSommationLRsEnMasseProcessor processor;
	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrService = getBean(ListeRecapService.class, "lrService");
		delaisService = getBean(DelaisService.class, "delaisService");
		lrDAO = getBean(ListeRecapitulativeDAO.class,"lrDAO");
		adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, delaisService, tiersService, adresseService);
	}

	/**
	 * [UNIREG-2003] teste la possibilité de restreindre le traitement des LRs à certaines catégories de débiteurs
	 */
	@Test
	public void testGetListIdLRsParCategories() throws Exception {

		class Ids {
			long admin;
			long cas;
			long hypo;
			long ltn;
			long lpp;
			long reg;
		}
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final DeclarationImpotSource admin = addLRaSommerAvecDebiteur(CategorieImpotSource.ADMINISTRATEURS, periode);
				final DeclarationImpotSource cas = addLRaSommerAvecDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, periode);
				final DeclarationImpotSource hypo = addLRaSommerAvecDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, periode);
				final DeclarationImpotSource ltn = addLRaSommerAvecDebiteur(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR, periode);
				final DeclarationImpotSource lpp = addLRaSommerAvecDebiteur(CategorieImpotSource.PRESTATIONS_PREVOYANCE, periode);
				final DeclarationImpotSource reg = addLRaSommerAvecDebiteur(CategorieImpotSource.REGULIERS, periode);

				final Ids ids = new Ids();
				ids.admin = admin.getId();
				ids.cas = cas.getId();
				ids.hypo = hypo.getId();
				ids.ltn = ltn.getId();
				ids.lpp = lpp.getId();
				ids.reg = reg.getId();
				return ids;
			}
		});

		// Pas de critère sur la catégorie de débiteur
		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, date(2010, 1, 1), null);
		assertNotNull(allIds);
		assertEquals(6, allIds.size());
		Collections.sort(allIds, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);

		assertEquals(ids.admin, allIds.get(0).getIdDeclaration());
		assertEquals(ids.cas, allIds.get(1).getIdDeclaration());
		assertEquals(ids.hypo, allIds.get(2).getIdDeclaration());
		assertEquals(ids.ltn, allIds.get(3).getIdDeclaration());
		assertEquals(ids.lpp, allIds.get(4).getIdDeclaration());
		assertEquals(ids.reg, allIds.get(5).getIdDeclaration());

		// Catégorie = administrateurs
		final List<IdentifiantDeclaration> idsAdmin = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.ADMINISTRATEURS);
		assertNotNull(idsAdmin);
		assertEquals(1, idsAdmin.size());
		assertEquals(ids.admin, idsAdmin.get(0).getIdDeclaration());

		// Catégorie = conférenciers artistes sportifs
		final List<IdentifiantDeclaration> idsCas = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS);
		assertNotNull(idsCas);
		assertEquals(1, idsCas.size());
		assertEquals(ids.cas, idsCas.get(0).getIdDeclaration());

		// Catégorie = créanciers hypothécaires
		final List<IdentifiantDeclaration> idsHypo = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CREANCIERS_HYPOTHECAIRES);
		assertNotNull(idsHypo);
		assertEquals(1, idsHypo.size());
		assertEquals(ids.hypo, idsHypo.get(0).getIdDeclaration());

		// Catégorie = loi travail au noir
		final List<IdentifiantDeclaration> idsLtn = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
		assertNotNull(idsLtn);
		assertEquals(1, idsLtn.size());
		assertEquals(ids.ltn, idsLtn.get(0).getIdDeclaration());

		// Catégorie = prestations prévoyance
		final List<IdentifiantDeclaration> idsLpp = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.PRESTATIONS_PREVOYANCE);
		assertNotNull(idsLpp);
		assertEquals(1, idsLpp.size());
		assertEquals(ids.lpp, idsLpp.get(0).getIdDeclaration());

		// Catégorie = réguliers
		final List<IdentifiantDeclaration> idsReg = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.REGULIERS);
		assertNotNull(idsReg);
		assertEquals(1, idsReg.size());
		assertEquals(ids.reg, idsReg.get(0).getIdDeclaration());
	}

	/**
	 * [UNIREG-2109] Vérifie qu'il est possible de restreindre les LRs traitées par trimestre
	 */
	@Test
	public void testGetListIdLRsParTrimestre() throws Exception {

		class Ids {
			long janvierMensuelle;
			long janvierTrimestrielle;
			long janvierSemestrielle;
			long janvierAnnuelle;
			long janvierUnique;
			long decembreMensuelle;
			long decembreTrimestrielle;
			long decembreSemestrielle;
			long decembreUnique;
		}
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final DeclarationImpotSource janvierMensuelle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.MENSUEL);
				final DeclarationImpotSource janvierTrimestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.TRIMESTRIEL);
				final DeclarationImpotSource janvierSemestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.SEMESTRIEL);
				final DeclarationImpotSource janvierAnnuelle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.ANNUEL);
				final DeclarationImpotSource janvierUnique = addLRaSommerAvecDebiteurPeriodiciteUnique(periode, date(2007, 1, 1), date(2007, 1, 1));

				final DeclarationImpotSource decembreMensuelle = addLRaSommerAvecDebiteur(periode, date(2007, 12, 1), PeriodiciteDecompte.MENSUEL);
				final DeclarationImpotSource decembreTrimestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 10, 1), PeriodiciteDecompte.TRIMESTRIEL);
				final DeclarationImpotSource decembreSemestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 7, 1), PeriodiciteDecompte.SEMESTRIEL);
				final DeclarationImpotSource decembreUnique = addLRaSommerAvecDebiteurPeriodiciteUnique(periode, date(2007, 12, 31), date(2007, 12, 31));

				final Ids ids = new Ids();
				ids.janvierMensuelle = janvierMensuelle.getId();
				ids.janvierTrimestrielle = janvierTrimestrielle.getId();
				ids.janvierSemestrielle = janvierSemestrielle.getId();
				ids.janvierAnnuelle = janvierAnnuelle.getId();
				ids.janvierUnique = janvierUnique.getId();
				ids.decembreMensuelle = decembreMensuelle.getId();
				ids.decembreTrimestrielle = decembreTrimestrielle.getId();
				ids.decembreSemestrielle = decembreSemestrielle.getId();
				ids.decembreUnique = decembreUnique.getId();
				return ids;
			}
		});
		
		
		// Aucun critère
		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, date(2008, 3, 1), null);
		assertNotNull(allIds);
		assertEquals(9, allIds.size());
		Collections.sort(allIds, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals(ids.janvierMensuelle, allIds.get(0).getIdDeclaration());
		assertEquals(ids.janvierTrimestrielle, allIds.get(1).getIdDeclaration());
		assertEquals(ids.janvierSemestrielle, allIds.get(2).getIdDeclaration());
		assertEquals(ids.janvierAnnuelle, allIds.get(3).getIdDeclaration());
		assertEquals(ids.janvierUnique, allIds.get(4).getIdDeclaration());
		assertEquals(ids.decembreMensuelle, allIds.get(5).getIdDeclaration());
		assertEquals(ids.decembreTrimestrielle, allIds.get(6).getIdDeclaration());
		assertEquals(ids.decembreSemestrielle, allIds.get(7).getIdDeclaration());
		assertEquals(ids.decembreUnique, allIds.get(8).getIdDeclaration());

		// 1er trimestre
		final List<IdentifiantDeclaration> premierTrimestre = processor.getListIdLRs(date(2007, 3, 31), date(2008, 3, 1), null);
		assertNotNull(premierTrimestre);
		assertEquals(3, premierTrimestre.size());
		Collections.sort(premierTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals(ids.janvierMensuelle, premierTrimestre.get(0).getIdDeclaration());
		assertEquals(ids.janvierTrimestrielle, premierTrimestre.get(1).getIdDeclaration());
		assertEquals(ids.janvierUnique, premierTrimestre.get(2).getIdDeclaration());

		// 2ème trimestre
		final List<IdentifiantDeclaration> deuxiemeTrimestre = processor.getListIdLRs(date(2007, 6, 30), date(2008, 3, 1), null);
		assertNotNull(deuxiemeTrimestre);
		assertEquals(4, deuxiemeTrimestre.size());
		Collections.sort(deuxiemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals(ids.janvierMensuelle, deuxiemeTrimestre.get(0).getIdDeclaration());
		assertEquals(ids.janvierTrimestrielle, deuxiemeTrimestre.get(1).getIdDeclaration());
		assertEquals(ids.janvierSemestrielle, deuxiemeTrimestre.get(2).getIdDeclaration());
		assertEquals(ids.janvierUnique, deuxiemeTrimestre.get(3).getIdDeclaration());

		// 3ème trimestre
		final List<IdentifiantDeclaration> troisiemeTrimestre = processor.getListIdLRs(date(2007, 9, 30), date(2008, 3, 1), null);
		assertNotNull(troisiemeTrimestre);
		assertEquals(4, troisiemeTrimestre.size());
		Collections.sort(troisiemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals(ids.janvierMensuelle, troisiemeTrimestre.get(0).getIdDeclaration());
		assertEquals(ids.janvierTrimestrielle, troisiemeTrimestre.get(1).getIdDeclaration());
		assertEquals(ids.janvierSemestrielle, troisiemeTrimestre.get(2).getIdDeclaration());
		assertEquals(ids.janvierUnique, troisiemeTrimestre.get(3).getIdDeclaration());

		// 4ème trimestre
		final List<IdentifiantDeclaration> quatriemeTrimestre = processor.getListIdLRs(date(2007, 12, 31), date(2008, 3, 1), null);
		assertNotNull(quatriemeTrimestre);
		assertEquals(9, quatriemeTrimestre.size());
		Collections.sort(quatriemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals(ids.janvierMensuelle, quatriemeTrimestre.get(0).getIdDeclaration());
		assertEquals(ids.janvierTrimestrielle, quatriemeTrimestre.get(1).getIdDeclaration());
		assertEquals(ids.janvierSemestrielle, quatriemeTrimestre.get(2).getIdDeclaration());
		assertEquals(ids.janvierAnnuelle, quatriemeTrimestre.get(3).getIdDeclaration());
		assertEquals(ids.janvierUnique, quatriemeTrimestre.get(4).getIdDeclaration());
		assertEquals(ids.decembreMensuelle, quatriemeTrimestre.get(5).getIdDeclaration());
		assertEquals(ids.decembreTrimestrielle, quatriemeTrimestre.get(6).getIdDeclaration());
		assertEquals(ids.decembreSemestrielle, quatriemeTrimestre.get(7).getIdDeclaration());
		assertEquals(ids.decembreUnique, quatriemeTrimestre.get(8).getIdDeclaration());
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(CategorieImpotSource categorie, PeriodeFiscale periode) {
		DebiteurPrestationImposable admin = addDebiteur(categorie, PeriodiciteDecompte.MENSUEL,  date(2007, 1, 1));
		addForDebiteur(admin, date(2007,1,1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
		DeclarationImpotSource lr = addLR(admin, date(2007, 1, 1), PeriodiciteDecompte.ANNUEL, periode);
		addEtatDeclarationEmise(lr, date(2008, 1, 5));
		addDelaiDeclaration(lr, date(2008, 1, 5), date(2008, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
		return lr;
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(PeriodeFiscale periode, RegDate debut, PeriodiciteDecompte periodicite) {
		final DebiteurPrestationImposable admin = addDebiteur(CategorieImpotSource.REGULIERS, periodicite, debut);
		addForDebiteur(admin, debut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
		final DeclarationImpotSource lr = addLR(admin, debut, periodicite, periode);
		final RegDate fin = periodicite.getFinPeriode(debut);
		addEtatDeclarationEmise(lr, fin.addDays(6));
		addDelaiDeclaration(lr, fin.addDays(6), fin.addMonths(1), EtatDelaiDocumentFiscal.ACCORDE);
		return lr;
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteurPeriodiciteUnique(PeriodeFiscale periode, RegDate debut, RegDate fin) {
		final DebiteurPrestationImposable admin = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.UNIQUE, debut);
		addForDebiteur(admin, debut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
		final DeclarationImpotSource lr = addLRPeriodiciteUnique(admin, debut, fin, periode);
		addEtatDeclarationEmise(lr, fin.addDays(6));
		addDelaiDeclaration(lr, fin.addDays(6), fin.addMonths(1), EtatDelaiDocumentFiscal.ACCORDE);
		return lr;
	}

	/**
	 * C'est un cas qui se produit beaucoup avec les débiteur "web" : ils envoient la LR avant même qu'on leur demande...
	 */
	@Test
	public void testNonSommationLrRetourneeAvantEmission() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(2007);
				final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(pf, date(2007, 1, 1), PeriodiciteDecompte.MENSUEL);
				addEtatDeclarationRetournee(lr, date(2007, 1, 12), "TEST");

				final RegDate dateEmission = lr.getDernierEtatDeclarationOfType(TypeEtatDeclaration.EMISE).getDateObtention();
				final RegDate dateRetour = lr.getDernierEtatDeclarationOfType(TypeEtatDeclaration.RETOURNEE).getDateObtention();
				assertTrue(dateEmission.isAfter(dateRetour));
				return null;
			}
		});

		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, RegDate.get(), null);
		assertEquals(0, allIds.size());
	}

	@Test
	public void testNonSommationLrDejaSommee() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(2007);
				final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(pf, date(2007, 1, 1), PeriodiciteDecompte.MENSUEL);
				addEtatDeclarationSommee(lr, date(2007, 3, 12), date(2007, 3, 15), null);
				return null;
			}
		});

		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, RegDate.get(), null);
		assertEquals(0, allIds.size());
	}

	@Test
	public void testErreurException() throws Exception {
		// processeur qui fait boom! au traitement d'une LR
		processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, delaisService, tiersService, adresseService) {
			@Override
			protected void traiteLR(DeclarationImpotSource lr, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
				throw new RuntimeException("Exception de test!");
			}
		};

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.TRIMESTRIEL);
				return lr.getTiers().getId();
			}
		});

		final EnvoiSommationLRsResults run = processor.run(null, null, RegDate.get(), null);
		assertNotNull(run);
		assertNotNull(run.sommationLREnErreurs);
		assertEquals(1, run.sommationLREnErreurs.size());

		final EnvoiSommationLRsResults.Erreur erreur = run.sommationLREnErreurs.get(0);
		assertNotNull(erreur);
		assertEquals(EnvoiSommationLRsResults.ErreurType.ROLLBACK.description(), erreur.getDescriptionRaison());
		assertEquals("Exception de test!", erreur.details);
		assertEquals(dpiId, erreur.noCtb);
	}

	@Test
	public void testDateEnvoiCourrierLrSommee() throws Exception {

		final long lrId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), PeriodiciteDecompte.TRIMESTRIEL);
				return lr.getId();
			}
		});

		final RegDate dateTraitement = date(2007, 6, 11);
		final EnvoiSommationLRsResults run = processor.run(null, null, dateTraitement, null);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotSource lrSommee = lrDAO.get(lrId);

				final EtatDeclarationSommee etatSomme = (EtatDeclarationSommee) lrSommee.getDernierEtatDeclaration();
				final RegDate dateObtention = etatSomme.getDateObtention();
				final RegDate attendu = dateObtention.addDays(3);
				final RegDate dateEnvoiCourrier = etatSomme.getDateEnvoiCourrier();

				assertEquals(dateTraitement, dateObtention);
				assertEquals(attendu, dateEnvoiCourrier);
				return null;
			}
		});
	}
}
