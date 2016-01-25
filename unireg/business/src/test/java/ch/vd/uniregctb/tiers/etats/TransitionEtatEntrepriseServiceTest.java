package ch.vd.uniregctb.tiers.etats;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.etats.transition.ToAbsorbeeTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToDissouteTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToEnFailliteTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToEnLiquidationTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToFondeeTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToInscriteRCTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.ToRadieeRCTransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public class TransitionEtatEntrepriseServiceTest extends WithoutSpringTest {

	private TiersDAO dao;

	@Before
	public void setUp() {
		dao = new TransitionEtatMockTiersDao();
	}

	private TransitionEtatEntrepriseServiceImpl createService(TiersDAO dao, Organisation organisation) throws Exception {
		TransitionEtatEntrepriseServiceImpl factory = new TransitionEtatEntrepriseServiceImpl();
		factory.setTiersDAO(dao);
		factory.setServiceOrganisation(new MockServiceOrganisationService(organisation));
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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 4);
		assertTransition(disponibles, TypeEtatEntreprise.EN_FAILLITE, ToEnFailliteTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.EN_LIQUIDATION, ToEnLiquidationTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.ABSORBEE, ToAbsorbeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.FONDEE, ToFondeeTransitionEtatEntreprise.class);
	}


	private void assertTransition(Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles, TypeEtatEntreprise toType, Class clazz) {
		TransitionEtatEntreprise transition = disponibles.get(toType);
		Assert.assertNotNull(transition);
		Assert.assertEquals(toType, transition.getType());
		Assert.assertTrue(transition.getClass().equals(clazz));
	}

	@Test
	public void testGetTransitionsDisponiblesEnFaillite() throws Exception {
		final EtatEntreprise actuel = new EtatEntreprise();
		actuel.setType(TypeEtatEntreprise.EN_FAILLITE);
		actuel.setDateObtention(date(2015, 6, 24));
		actuel.setGeneration(TypeGenerationEtatEntreprise.MANUELLE);

		final Entreprise entreprise = new Entreprise(1234);
		actuel.setEntreprise(entreprise);
		entreprise.addEtat(actuel);

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 2);
		assertTransition(disponibles, TypeEtatEntreprise.RADIEE_RC, ToRadieeRCTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

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

		Organisation organisation = MockOrganisationFactory
				.createOrganisation(1L, 1L, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
				                    TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);


		TransitionEtatEntrepriseServiceImpl service = createService(dao, organisation);

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

		Organisation organisation = MockOrganisationFactory
				.createOrganisation(1L, 1L, "Assoc SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0109_ASSOCIATION,
				                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
				                    TypeOrganisationRegistreIDE.ASSOCIATION);


		TransitionEtatEntrepriseServiceImpl service = createService(dao, organisation);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 0);
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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 1);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 3);
		assertTransition(disponibles, TypeEtatEntreprise.ABSORBEE, ToAbsorbeeTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.DISSOUTE, ToDissouteTransitionEtatEntreprise.class);
		assertTransition(disponibles, TypeEtatEntreprise.INSCRITE_RC, ToInscriteRCTransitionEtatEntreprise.class);
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

		TransitionEtatEntrepriseServiceImpl service = createService(dao, null);

		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = service.getTransitionsDisponibles(entreprise, date(2015, 12, 31), TypeGenerationEtatEntreprise.MANUELLE);

		Assert.assertNotNull(disponibles);
		Assert.assertTrue(disponibles.size() == 0);
	}


	private static class MockServiceOrganisationService implements ServiceOrganisationService {
		private Organisation organisation;

		public MockServiceOrganisationService(Organisation organisation) {
			this.organisation = organisation;
		}

		@Override
		public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
			return organisation;
		}

		@Override
		public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AdressesCivilesHistoriques getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@NotNull
		@Override
		public String createOrganisationDescription(Organisation organisation, RegDate date) {
			throw new UnsupportedOperationException();
		}
	}

}