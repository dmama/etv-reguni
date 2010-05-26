package ch.vd.uniregctb.declaration.source;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class EnvoiSommationLRsEnMasseProcessorTest extends BusinessTest {

	private EnvoiSommationLRsEnMasseProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ListeRecapService lrService = getBean(ListeRecapService.class, "lrService");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");

		// création du processeur à la main pour pouvoir accéder aux méthodes protégées
		processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, lrService, delaisService);
	}

	/**
	 * [UNIREG-2003] teste la possibilité de restreindre le traitement des LRs à certaines catégories de débiteurs
	 */
	@Test
	public void testGetListIdLRsParCategories() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		DeclarationImpotSource admin = addLRaSommerAvecDebiteur(CategorieImpotSource.ADMINISTRATEURS, periode);
		DeclarationImpotSource cas = addLRaSommerAvecDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, periode);
		DeclarationImpotSource hypo = addLRaSommerAvecDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, periode);
		DeclarationImpotSource ltn = addLRaSommerAvecDebiteur(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR, periode);
		DeclarationImpotSource lpp = addLRaSommerAvecDebiteur(CategorieImpotSource.PRESTATIONS_PREVOYANCE, periode);
		DeclarationImpotSource reg = addLRaSommerAvecDebiteur(CategorieImpotSource.REGULIERS, periode);
		hibernateTemplate.flush();

		// Pas de critère sur la catégorie de débiteur
		final List<Long> allIds = processor.getListIdLRs(null, date(2010, 1, 1), null);
		assertNotNull(allIds);
		assertEquals(6, allIds.size());
		Collections.sort(allIds);

		assertEquals(admin.getId(), allIds.get(0));
		assertEquals(cas.getId(), allIds.get(1));
		assertEquals(hypo.getId(), allIds.get(2));
		assertEquals(ltn.getId(), allIds.get(3));
		assertEquals(lpp.getId(), allIds.get(4));
		assertEquals(reg.getId(), allIds.get(5));

		// Catégorie = administrateurs
		final List<Long> idsAdmin = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.ADMINISTRATEURS);
		assertNotNull(idsAdmin);
		assertEquals(1, idsAdmin.size());
		assertEquals(admin.getId(), idsAdmin.get(0));

		// Catégorie = conférenciers artistes sportifs
		final List<Long> idsCas = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS);
		assertNotNull(idsCas);
		assertEquals(1, idsCas.size());
		assertEquals(cas.getId(), idsCas.get(0));

		// Catégorie = créanciers hypothécaires
		final List<Long> idsHypo = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.CREANCIERS_HYPOTHECAIRES);
		assertNotNull(idsHypo);
		assertEquals(1, idsHypo.size());
		assertEquals(hypo.getId(), idsHypo.get(0));

		// Catégorie = loi travail au noir
		final List<Long> idsLtn = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
		assertNotNull(idsLtn);
		assertEquals(1, idsLtn.size());
		assertEquals(ltn.getId(), idsLtn.get(0));

		// Catégorie = prestations prévoyance
		final List<Long> idsLpp = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.PRESTATIONS_PREVOYANCE);
		assertNotNull(idsLpp);
		assertEquals(1, idsLpp.size());
		assertEquals(lpp.getId(), idsLpp.get(0));

		// Catégorie = réguliers
		final List<Long> idsReg = processor.getListIdLRs(null, date(2010, 1, 1), CategorieImpotSource.REGULIERS);
		assertNotNull(idsReg);
		assertEquals(1, idsReg.size());
		assertEquals(reg.getId(), idsReg.get(0));
	}

	/**
	 * [UNIREG-2109] Vérifie qu'il est possible de restreindre les LRs traitées par trimestre
	 */
	@Test
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
		final List<Long> allIds = processor.getListIdLRs(null, date(2008, 3, 1), null);
		assertNotNull(allIds);
		assertEquals(9, allIds.size());
		Collections.sort(allIds);
		assertEquals(janvierMensuelle.getId(), allIds.get(0));
		assertEquals(janvierTrimestrielle.getId(), allIds.get(1));
		assertEquals(janvierSemestrielle.getId(), allIds.get(2));
		assertEquals(janvierAnnuelle.getId(), allIds.get(3));
		assertEquals(janvierUnique.getId(), allIds.get(4));
		assertEquals(decembreMensuelle.getId(), allIds.get(5));
		assertEquals(decembreTrimestrielle.getId(), allIds.get(6));
		assertEquals(decembreSemestrielle.getId(), allIds.get(7));
		assertEquals(decembreUniques.getId(), allIds.get(8));

		// 1er trimestre
		final List<Long> premierTrimestre = processor.getListIdLRs(date(2007, 3, 31), date(2008, 3, 1), null);
		assertNotNull(premierTrimestre);
		assertEquals(3, premierTrimestre.size());
		Collections.sort(premierTrimestre);
		assertEquals(janvierMensuelle.getId(), premierTrimestre.get(0));
		assertEquals(janvierTrimestrielle.getId(), premierTrimestre.get(1));
		assertEquals(janvierUnique.getId(), premierTrimestre.get(2));

		// 2ème trimestre
		final List<Long> deuxiemeTrimestre = processor.getListIdLRs(date(2007, 6, 30), date(2008, 3, 1), null);
		assertNotNull(deuxiemeTrimestre);
		assertEquals(4, deuxiemeTrimestre.size());
		Collections.sort(deuxiemeTrimestre);
		assertEquals(janvierMensuelle.getId(), deuxiemeTrimestre.get(0));
		assertEquals(janvierTrimestrielle.getId(), deuxiemeTrimestre.get(1));
		assertEquals(janvierSemestrielle.getId(), deuxiemeTrimestre.get(2));
		assertEquals(janvierUnique.getId(), deuxiemeTrimestre.get(3));

		// 3ème trimestre
		final List<Long> troisiemeTrimestre = processor.getListIdLRs(date(2007, 9, 30), date(2008, 3, 1), null);
		assertNotNull(troisiemeTrimestre);
		assertEquals(4, troisiemeTrimestre.size());
		Collections.sort(troisiemeTrimestre);
		assertEquals(janvierMensuelle.getId(), troisiemeTrimestre.get(0));
		assertEquals(janvierTrimestrielle.getId(), troisiemeTrimestre.get(1));
		assertEquals(janvierSemestrielle.getId(), troisiemeTrimestre.get(2));
		assertEquals(janvierUnique.getId(), troisiemeTrimestre.get(3));

		// 4ème trimestre
		final List<Long> quatriemeTrimestre = processor.getListIdLRs(date(2007, 12, 31), date(2008, 3, 1), null);
		assertNotNull(quatriemeTrimestre);
		assertEquals(9, quatriemeTrimestre.size());
		Collections.sort(quatriemeTrimestre);
		assertEquals(janvierMensuelle.getId(), quatriemeTrimestre.get(0));
		assertEquals(janvierTrimestrielle.getId(), quatriemeTrimestre.get(1));
		assertEquals(janvierSemestrielle.getId(), quatriemeTrimestre.get(2));
		assertEquals(janvierAnnuelle.getId(), quatriemeTrimestre.get(3));
		assertEquals(janvierUnique.getId(), quatriemeTrimestre.get(4));
		assertEquals(decembreMensuelle.getId(), quatriemeTrimestre.get(5));
		assertEquals(decembreTrimestrielle.getId(), quatriemeTrimestre.get(6));
		assertEquals(decembreSemestrielle.getId(), quatriemeTrimestre.get(7));
		assertEquals(decembreUniques.getId(), quatriemeTrimestre.get(8));
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(CategorieImpotSource categorie, PeriodeFiscale periode) {
		DebiteurPrestationImposable admin = addDebiteur(categorie, PeriodiciteDecompte.MENSUEL);
		DeclarationImpotSource lr = addLR(admin, date(2007, 1, 1), date(2007, 12, 31), periode);
		addEtatDeclaration(lr, date(2008, 1, 5), TypeEtatDeclaration.EMISE);
		addDelaiDeclaration(lr, date(2008, 1, 5), date(2008, 3, 15));
		return lr;
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(PeriodeFiscale periode, RegDate debut, RegDate fin, PeriodiciteDecompte periodicite) {
		DebiteurPrestationImposable admin = addDebiteur(CategorieImpotSource.REGULIERS, periodicite);
		DeclarationImpotSource lr = addLR(admin, debut, fin, periode);
		addEtatDeclaration(lr, fin.addDays(6), TypeEtatDeclaration.EMISE);
		addDelaiDeclaration(lr, fin.addDays(6), fin.addMonths(1));
		return lr;
	}
}
