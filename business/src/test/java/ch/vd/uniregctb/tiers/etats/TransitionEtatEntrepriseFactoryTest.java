package ch.vd.uniregctb.tiers.etats;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToEnLiquidationTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public class TransitionEtatEntrepriseFactoryTest extends WithoutSpringTest {

	private TransitionEtatMockTiersDao dao;
	private EvenementFiscalMockService evtFiscService;

	@Before
	public void setUp() throws Exception {
		dao = new TransitionEtatMockTiersDao();
		evtFiscService = new EvenementFiscalMockService();
	}

	@Test
	public void testToEnLiquidationTransition() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.INSCRITE_RC);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseFactory factory = new ToEnLiquidationTransitionEtatEntrepriseFactory(dao, evtFiscService);

		TransitionEtatEntreprise transition = factory.create(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);
		Assert.assertNotNull(transition);

		EtatEntreprise nouvel = transition.apply();

		Assert.assertNotNull(nouvel);

		EtatEntreprise dernierEtat = CollectionsUtils.getLastElement(entreprise.getEtatsNonAnnulesTries());
		Assert.assertEquals(nouvel, dernierEtat);

		Assert.assertEquals(nouvel, dao.getNouvelEtatEntreprise());
		Assert.assertEquals(entreprise, dao.getEntreprise());

		Assert.assertEquals(TypeEtatEntreprise.EN_LIQUIDATION, nouvel.getType());
		Assert.assertEquals(date(2015, 12, 31), nouvel.getDateObtention());
		Assert.assertEquals(entreprise, nouvel.getEntreprise());
		Assert.assertEquals(TypeGenerationEtatEntreprise.MANUELLE, nouvel.getGeneration());

		Assert.assertTrue(evtFiscService.isCalledOnce());
		Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.LIQUIDATION, evtFiscService.getType());
		Assert.assertEquals(date(2015, 12, 31), evtFiscService.getDateEvenement());
		Assert.assertEquals(entreprise, evtFiscService.getEntreprise());
	}

	@Test
	public void testDateInvalide() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.INSCRITE_RC);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseFactory factory = new ToEnLiquidationTransitionEtatEntrepriseFactory(dao, evtFiscService);

		TransitionEtatEntreprise transition = factory.create(entreprise, date(2014, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);
		Assert.assertNull(transition);

		EtatEntreprise dernierEtat = CollectionsUtils.getLastElement(entreprise.getEtatsNonAnnulesTries());
		Assert.assertEquals(actuel, dernierEtat);

		Assert.assertFalse(evtFiscService.isCalledOnce());
	}
}