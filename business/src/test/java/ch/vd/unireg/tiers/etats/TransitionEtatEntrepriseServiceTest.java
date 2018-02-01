package ch.vd.unireg.tiers.etats;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.etats.transition.ToAbsorbeeTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToDissouteTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToEnFailliteTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToEnLiquidationTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToFondeeTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToInscriteRCTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.ToRadieeRCTransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public class TransitionEtatEntrepriseServiceTest extends WithoutSpringTest {

	private TiersDAO dao;

	@Before
	public void setUp() {
		dao = new TransitionEtatMockTiersDao();
	}

	private TransitionEtatEntrepriseServiceImpl createService(TiersDAO dao) throws Exception {
		final TransitionEtatEntrepriseServiceImpl factory = new TransitionEtatEntrepriseServiceImpl();
		factory.setTiersDAO(dao);
		final EvenementFiscalMockService evenementFiscalMockService = new EvenementFiscalMockService();
		factory.setEvenementFiscalService(evenementFiscalMockService);
		factory.afterPropertiesSet();
		return factory;
	}

	@Test
	public void testGetTransitionPourEtat() throws Exception {

		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.INSCRITE_RC);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		TransitionEtatEntreprise translator = service.getTransitionVersEtat(TypeEtatEntreprise.EN_LIQUIDATION, entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(translator);
		Assert.assertTrue(translator instanceof ToEnLiquidationTransitionEtatEntreprise);
	}

	@Test
	public void testGetTransitionsDisponiblesInscriteRC() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.INSCRITE_RC);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 5);
		assertTransition(disponibles, TypeEtatEntreprise.EN_FAILLITE, ToEnFailliteTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.EN_LIQUIDATION, ToEnLiquidationTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.ABSORBEE, ToAbsorbeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesInscriteRCEtatVide() throws Exception {

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.setNumeroEntreprise(1L);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 2);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesNonRCEtatVide() throws Exception {

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.setNumeroEntreprise(1L);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 2);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
	}

	private void assertTransition(Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles, TypeEtatEntreprise toType, Class clazz) {
		TransitionEtatEntreprise transition = disponibles.get(toType);
		Assert.assertNotNull(transition);
		Assert.assertEquals(toType, transition.getTypeDestination());
		Assert.assertTrue(transition.getClass().equals(clazz));
	}

	@Test
	public void testGetTransitionsDisponiblesEnFaillite() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.EN_FAILLITE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.setNumeroEntreprise(1L);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 4);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.DISSOUTE, ToDissouteTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesEnFailliteNonRC() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.EN_FAILLITE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 4);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.DISSOUTE, ToDissouteTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesEnLiquidation() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.EN_LIQUIDATION);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 3);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.EN_FAILLITE, ToEnFailliteTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesAbsorbée() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.ABSORBEE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.setNumeroEntreprise(1L);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 1);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesAbsorbéeNonRC() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.ABSORBEE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		entreprise.setNumeroEntreprise(1L);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 1);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesRadieeRC() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.RADIEE_RC);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 3);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.EN_FAILLITE, ToEnFailliteTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesFondee() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.FONDEE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 4);
		assertTransition(disponibles, TypeEtatEntreprise.ABSORBEE, ToAbsorbeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.DISSOUTE, ToDissouteTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.EN_FAILLITE, ToEnFailliteTransitionEtatEntreprise.class);
	}

	@Test
	public void testGetTransitionsDisponiblesDissoute() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.DISSOUTE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 0);
	}
}