package ch.vd.uniregctb.declaration.source;

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

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
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
		final List<Long> allIds = processor.getListIdLRs(date(2010, 1, 1), null);
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
		final List<Long> idsAdmin = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.ADMINISTRATEURS);
		assertNotNull(idsAdmin);
		assertEquals(1, idsAdmin.size());
		assertEquals(admin.getId(), idsAdmin.get(0));

		// Catégorie = conférenciers artistes sportifs
		final List<Long> idsCas = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS);
		assertNotNull(idsCas);
		assertEquals(1, idsCas.size());
		assertEquals(cas.getId(), idsCas.get(0));

		// Catégorie = créanciers hypothécaires
		final List<Long> idsHypo = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.CREANCIERS_HYPOTHECAIRES);
		assertNotNull(idsHypo);
		assertEquals(1, idsHypo.size());
		assertEquals(hypo.getId(), idsHypo.get(0));

		// Catégorie = loi travail au noir
		final List<Long> idsLtn = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
		assertNotNull(idsLtn);
		assertEquals(1, idsLtn.size());
		assertEquals(ltn.getId(), idsLtn.get(0));

		// Catégorie = prestations prévoyance
		final List<Long> idsLpp = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.PRESTATIONS_PREVOYANCE);
		assertNotNull(idsLpp);
		assertEquals(1, idsLpp.size());
		assertEquals(lpp.getId(), idsLpp.get(0));

		// Catégorie = réguliers
		final List<Long> idsReg = processor.getListIdLRs(date(2010, 1, 1), CategorieImpotSource.REGULIERS);
		assertNotNull(idsReg);
		assertEquals(1, idsReg.size());
		assertEquals(reg.getId(), idsReg.get(0));
	}

	private DeclarationImpotSource addLRaSommerAvecDebiteur(CategorieImpotSource categorie, PeriodeFiscale periode) {
		DebiteurPrestationImposable admin = addDebiteur(categorie, PeriodiciteDecompte.MENSUEL);
		DeclarationImpotSource lr = addLR(admin, date(2007, 1, 1), date(2007, 12, 31), periode);
		addEtatDeclaration(lr, date(2008, 1, 5), TypeEtatDeclaration.EMISE);
		addDelaiDeclaration(lr, date(2008, 1, 5), date(2008, 3, 15));
		return lr;
	}
}
