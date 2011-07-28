package ch.vd.uniregctb.declaration.source;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
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

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrService = getBean(ListeRecapService.class, "lrService");
		delaisService = getBean(DelaisService.class, "delaisService");
		lrDAO = getBean(ListeRecapitulativeDAO.class,"lrDAO");


		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, delaisService);
	}

	/**
	 * [UNIREG-2003] teste la possibilité de restreindre le traitement des LRs à certaines catégories de débiteurs
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetListIdLRsParCategories() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		final DeclarationImpotSource admin = addLRaSommerAvecDebiteur(CategorieImpotSource.ADMINISTRATEURS, periode);
		final DeclarationImpotSource cas = addLRaSommerAvecDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, periode);
		final DeclarationImpotSource hypo = addLRaSommerAvecDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, periode);
		final DeclarationImpotSource ltn = addLRaSommerAvecDebiteur(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR, periode);
		final DeclarationImpotSource lpp = addLRaSommerAvecDebiteur(CategorieImpotSource.PRESTATIONS_PREVOYANCE, periode);
		final DeclarationImpotSource reg = addLRaSommerAvecDebiteur(CategorieImpotSource.REGULIERS, periode);
		hibernateTemplate.flush();

		// Pas de critère sur la catégorie de débiteur
		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, date(2010, 1, 1), null);
		assertNotNull(allIds);
		assertEquals(6, allIds.size());
		Collections.sort(allIds, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);

		assertEquals((long) admin.getId(), allIds.get(0).getIdDeclaration());
		assertEquals((long) cas.getId(), allIds.get(1).getIdDeclaration());
		assertEquals((long) hypo.getId(), allIds.get(2).getIdDeclaration());
		assertEquals((long) ltn.getId(), allIds.get(3).getIdDeclaration());
		assertEquals((long) lpp.getId(), allIds.get(4).getIdDeclaration());
		assertEquals((long) reg.getId(), allIds.get(5).getIdDeclaration());

		// Catégorie = administrateurs
		final List<IdentifiantDeclaration> idsAdmin = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.ADMINISTRATEURS);
		assertNotNull(idsAdmin);
		assertEquals(1, idsAdmin.size());
		assertEquals((long) admin.getId(), idsAdmin.get(0).getIdDeclaration());

		// Catégorie = conférenciers artistes sportifs
		final List<IdentifiantDeclaration> idsCas = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS);
		assertNotNull(idsCas);
		assertEquals(1, idsCas.size());
		assertEquals((long) cas.getId(), idsCas.get(0).getIdDeclaration());

		// Catégorie = créanciers hypothécaires
		final List<IdentifiantDeclaration> idsHypo = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CREANCIERS_HYPOTHECAIRES);
		assertNotNull(idsHypo);
		assertEquals(1, idsHypo.size());
		assertEquals((long) hypo.getId(), idsHypo.get(0).getIdDeclaration());

		// Catégorie = loi travail au noir
		final List<IdentifiantDeclaration> idsLtn = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
		assertNotNull(idsLtn);
		assertEquals(1, idsLtn.size());
		assertEquals((long) ltn.getId(), idsLtn.get(0).getIdDeclaration());

		// Catégorie = prestations prévoyance
		final List<IdentifiantDeclaration> idsLpp = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.PRESTATIONS_PREVOYANCE);
		assertNotNull(idsLpp);
		assertEquals(1, idsLpp.size());
		assertEquals((long) lpp.getId(), idsLpp.get(0).getIdDeclaration());

		// Catégorie = réguliers
		final List<IdentifiantDeclaration> idsReg = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.REGULIERS);
		assertNotNull(idsReg);
		assertEquals(1, idsReg.size());
		assertEquals((long) reg.getId(), idsReg.get(0).getIdDeclaration());
	}

	/**
	 * [UNIREG-2109] Vérifie qu'il est possible de restreindre les LRs traitées par trimestre
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetListIdLRsParTrimestre() {

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		final DeclarationImpotSource janvierMensuelle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 1, 31), PeriodiciteDecompte.MENSUEL);
		final DeclarationImpotSource janvierTrimestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 3, 31), PeriodiciteDecompte.TRIMESTRIEL);
		final DeclarationImpotSource janvierSemestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 6, 30), PeriodiciteDecompte.SEMESTRIEL);
		final DeclarationImpotSource janvierAnnuelle = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 12, 31), PeriodiciteDecompte.ANNUEL);
		final DeclarationImpotSource janvierUnique = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 1, 1), PeriodiciteDecompte.UNIQUE);

		final DeclarationImpotSource decembreMensuelle = addLRaSommerAvecDebiteur(periode, date(2007, 12, 1), date(2007, 12, 31), PeriodiciteDecompte.MENSUEL);
		final DeclarationImpotSource decembreTrimestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 10, 1), date(2007, 12, 31), PeriodiciteDecompte.TRIMESTRIEL);
		final DeclarationImpotSource decembreSemestrielle = addLRaSommerAvecDebiteur(periode, date(2007, 7, 1), date(2007, 12, 31), PeriodiciteDecompte.SEMESTRIEL);
		final DeclarationImpotSource decembreUniques = addLRaSommerAvecDebiteur(periode, date(2007, 12, 31), date(2007, 12, 31), PeriodiciteDecompte.UNIQUE);
		
		// Aucun de critère
		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, date(2008, 3, 1), null);
		assertNotNull(allIds);
		assertEquals(9, allIds.size());
		Collections.sort(allIds, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals((long) janvierMensuelle.getId(), allIds.get(0).getIdDeclaration());
		assertEquals((long) janvierTrimestrielle.getId(), allIds.get(1).getIdDeclaration());
		assertEquals((long) janvierSemestrielle.getId(), allIds.get(2).getIdDeclaration());
		assertEquals((long) janvierAnnuelle.getId(), allIds.get(3).getIdDeclaration());
		assertEquals((long) janvierUnique.getId(), allIds.get(4).getIdDeclaration());
		assertEquals((long) decembreMensuelle.getId(), allIds.get(5).getIdDeclaration());
		assertEquals((long) decembreTrimestrielle.getId(), allIds.get(6).getIdDeclaration());
		assertEquals((long) decembreSemestrielle.getId(), allIds.get(7).getIdDeclaration());
		assertEquals((long) decembreUniques.getId(), allIds.get(8).getIdDeclaration());

		// 1er trimestre
		final List<IdentifiantDeclaration> premierTrimestre = processor.getListIdLRs(date(2007, 3, 31), date(2008, 3, 1), null);
		assertNotNull(premierTrimestre);
		assertEquals(3, premierTrimestre.size());
		Collections.sort(premierTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals((long) janvierMensuelle.getId(), premierTrimestre.get(0).getIdDeclaration());
		assertEquals((long) janvierTrimestrielle.getId(), premierTrimestre.get(1).getIdDeclaration());
		assertEquals((long) janvierUnique.getId(), premierTrimestre.get(2).getIdDeclaration());

		// 2ème trimestre
		final List<IdentifiantDeclaration> deuxiemeTrimestre = processor.getListIdLRs(date(2007, 6, 30), date(2008, 3, 1), null);
		assertNotNull(deuxiemeTrimestre);
		assertEquals(4, deuxiemeTrimestre.size());
		Collections.sort(deuxiemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals((long) janvierMensuelle.getId(), deuxiemeTrimestre.get(0).getIdDeclaration());
		assertEquals((long) janvierTrimestrielle.getId(), deuxiemeTrimestre.get(1).getIdDeclaration());
		assertEquals((long) janvierSemestrielle.getId(), deuxiemeTrimestre.get(2).getIdDeclaration());
		assertEquals((long) janvierUnique.getId(), deuxiemeTrimestre.get(3).getIdDeclaration());

		// 3ème trimestre
		final List<IdentifiantDeclaration> troisiemeTrimestre = processor.getListIdLRs(date(2007, 9, 30), date(2008, 3, 1), null);
		assertNotNull(troisiemeTrimestre);
		assertEquals(4, troisiemeTrimestre.size());
		Collections.sort(troisiemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals((long) janvierMensuelle.getId(), troisiemeTrimestre.get(0).getIdDeclaration());
		assertEquals((long) janvierTrimestrielle.getId(), troisiemeTrimestre.get(1).getIdDeclaration());
		assertEquals((long) janvierSemestrielle.getId(), troisiemeTrimestre.get(2).getIdDeclaration());
		assertEquals((long) janvierUnique.getId(), troisiemeTrimestre.get(3).getIdDeclaration());

		// 4ème trimestre
		final List<IdentifiantDeclaration> quatriemeTrimestre = processor.getListIdLRs(date(2007, 12, 31), date(2008, 3, 1), null);
		assertNotNull(quatriemeTrimestre);
		assertEquals(9, quatriemeTrimestre.size());
		Collections.sort(quatriemeTrimestre, IdentifiantDeclaration.COMPARATOR_BY_DECL_ID);
		assertEquals((long) janvierMensuelle.getId(), quatriemeTrimestre.get(0).getIdDeclaration());
		assertEquals((long) janvierTrimestrielle.getId(), quatriemeTrimestre.get(1).getIdDeclaration());
		assertEquals((long) janvierSemestrielle.getId(), quatriemeTrimestre.get(2).getIdDeclaration());
		assertEquals((long) janvierAnnuelle.getId(), quatriemeTrimestre.get(3).getIdDeclaration());
		assertEquals((long) janvierUnique.getId(), quatriemeTrimestre.get(4).getIdDeclaration());
		assertEquals((long) decembreMensuelle.getId(), quatriemeTrimestre.get(5).getIdDeclaration());
		assertEquals((long) decembreTrimestrielle.getId(), quatriemeTrimestre.get(6).getIdDeclaration());
		assertEquals((long) decembreSemestrielle.getId(), quatriemeTrimestre.get(7).getIdDeclaration());
		assertEquals((long) decembreUniques.getId(), quatriemeTrimestre.get(8).getIdDeclaration());
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(CategorieImpotSource categorie, PeriodeFiscale periode) {
		DebiteurPrestationImposable admin = addDebiteur(categorie, PeriodiciteDecompte.MENSUEL,  date(2007, 1, 1));
		DeclarationImpotSource lr = addLR(admin, date(2007, 1, 1), date(2007, 12, 31), periode);
		addEtatDeclarationEmise(lr, date(2008, 1, 5));
		addDelaiDeclaration(lr, date(2008, 1, 5), date(2008, 3, 15));
		return lr;
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(PeriodeFiscale periode, RegDate debut, RegDate fin, PeriodiciteDecompte periodicite) {
		DebiteurPrestationImposable admin = addDebiteur(CategorieImpotSource.REGULIERS, periodicite, debut);
		DeclarationImpotSource lr = addLR(admin, debut, fin, periode);
		addEtatDeclarationEmise(lr, fin.addDays(6));
		addDelaiDeclaration(lr, fin.addDays(6), fin.addMonths(1));
		return lr;
	}

	/**
	 * C'est un cas qui se produit beaucoup avec les débiteur "web" : ils envoient la LR avant même qu'on leur demande...
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonSommationLrRetourneeAvantEmission() throws Exception {
		final PeriodeFiscale pf = addPeriodeFiscale(2007);
		final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(pf, date(2007, 1, 1), date(2007, 1, 31), PeriodiciteDecompte.MENSUEL);
		lr.addEtat(new EtatDeclarationRetournee(date(2007, 1, 12)));

		final RegDate dateEmission = lr.getEtatDeclarationActif(TypeEtatDeclaration.EMISE).getDateObtention();
		final RegDate dateRetour = lr.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE).getDateObtention();
		assertTrue(dateEmission.isAfter(dateRetour));

		hibernateTemplate.flush();

		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, RegDate.get(), null);
		assertEquals(0, allIds.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonSommationLrDejaSommee() throws Exception {
		final PeriodeFiscale pf = addPeriodeFiscale(2007);
		final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(pf, date(2007, 1, 1), date(2007, 1, 31), PeriodiciteDecompte.MENSUEL);
		lr.addEtat(new EtatDeclarationSommee(date(2007, 3, 12),date(2007, 3, 15)));
		hibernateTemplate.flush();

		final List<IdentifiantDeclaration> allIds = processor.getListIdLRs(null, RegDate.get(), null);
		assertEquals(0, allIds.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testErreurException() throws Exception {
		// processeur qui fait boom! au traitement d'une LR
		processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, delaisService) {
			@Override
			protected void traiteLR(DeclarationImpotSource lr, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
				throw new RuntimeException("Exception de test!");
			}
		};

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 3, 31), PeriodiciteDecompte.TRIMESTRIEL);
		hibernateTemplate.flush();

		final EnvoiSommationLRsResults run = processor.run(null, null, RegDate.get(), null);
		assertNotNull(run);
		assertNotNull(run.sommationLREnErreurs);
		assertEquals(1, run.sommationLREnErreurs.size());

		final EnvoiSommationLRsResults.Erreur erreur = run.sommationLREnErreurs.get(0);
		assertNotNull(erreur);
		assertEquals(EnvoiSommationLRsResults.ErreurType.ROLLBACK.description(), erreur.getDescriptionRaison());
		assertEquals("Exception de test!", erreur.details);
		assertEquals((long) lr.getTiers().getNumero(), erreur.noCtb);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateEnvoiCourrierLrSommee() throws Exception {


		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		final DeclarationImpotSource lr = addLRaSommerAvecDebiteur(periode, date(2007, 1, 1), date(2007, 3, 31), PeriodiciteDecompte.TRIMESTRIEL);
		hibernateTemplate.flush();

		final RegDate dateTraitement = date(2007, 6, 11);
		final EnvoiSommationLRsResults run = processor.run(null, null, dateTraitement, null);

		final DeclarationImpotSource lrSommee = lrDAO.get(lr.getId());

		EtatDeclarationSommee etatSomme = (EtatDeclarationSommee)lrSommee.getDernierEtat();

		final RegDate dateObtention = etatSomme.getDateObtention();
		final RegDate attendu = dateObtention.addDays(3);
		final RegDate dateEnvoiCourrier =  etatSomme.getDateEnvoiCourrier();

		assertEquals(dateTraitement,dateObtention);
		assertEquals(attendu,dateEnvoiCourrier);




	}
}
