package ch.vd.unireg.evenement.civil.interne;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.data.FiscalDataEventListener;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.arrivee.ArriveePrincipale;
import ch.vd.unireg.evenement.civil.interne.mariage.Mariage;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class EvenementCivilInterneTest extends WithoutSpringTest {

	private AuditManager audit = Mockito.mock(AuditManager.class);

	@Test
	public void testInitCouple() throws Exception {

		/*
		 * Création des données des services
		 */
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;

		final MockTiersDAO tiersDAO = new MockTiersDAO();
		final ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService(), tiersDAO);
		final ServiceCivilService serviceCivil = new ServiceCivilImpl(infrastructureService, new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				final RegDate dateArrivee = RegDate.get(1980, 1, 1);	// sans importance
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 7, 11));
			}
		});

		/*
		 * Création d'un événement civil composé de deux individus
		 */
		final RegDate dateEvenement = RegDate.get(2008, 3, 10);
		final EvenementCivilRegPP evenementArriveeCouple = new EvenementCivilRegPP(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE,
				EtatEvenementCivil.A_TRAITER, dateEvenement, noIndividuPrincipal,
				noIndividuConjoint, MockCommune.Lausanne.getNoOFS(), null);

		/*
		 * Création et initialisation de l'adapter
		 */
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO, audit);
		final EvenementCivilOptions options = new EvenementCivilOptions(false);
		final EvenementCivilInterne adapter = new ArriveePrincipale(evenementArriveeCouple, context, options);

		/*
		 * Test de la méthode init dans ce cas
		 */
		Assert.assertNotNull(adapter.getNoIndividu());
		Assert.assertEquals(noIndividuPrincipal, adapter.getNoIndividu().longValue());
		Assert.assertNotNull(adapter.getNoIndividuConjoint());
		Assert.assertEquals(noIndividuConjoint, adapter.getNoIndividuConjoint().longValue());
		Assert.assertEquals(dateEvenement, adapter.getDate());
		Assert.assertNotNull(adapter.getNumeroOfsCommuneAnnonce());
		Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), adapter.getNumeroOfsCommuneAnnonce().intValue());
	}

	@Test
	public void testInitAvecRefreshCacheIndividu() throws Exception {

		final long noIndMonsieur = 1L;
		final long noIndMadame = 2L;
		final RegDate dateMariage = RegDate.get(1985, 7, 11);

		final MockTiersDAO tiersDAO = new MockTiersDAO();
		final ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService(), tiersDAO);
		final ServiceCivilService serviceCivil = new ServiceCivilImpl(infrastructureService, new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndMonsieur, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndMadame, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				final RegDate dateArrivee = RegDate.get(1980, 1, 1);	// sans importance
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);

				// marie les individus
				marieIndividus(pierre, julie, dateMariage);
			}
		});
		final MyDataEventService dataEventService = new MyDataEventService();

		/*
		 * Création d'un événement civil de mariage
		 */
		final EvenementCivilRegPP
				evtMariage = new EvenementCivilRegPP(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER, dateMariage, noIndMonsieur, null, MockCommune.Lausanne.getNoOFS(), null);

		// passage dans l'init de l'adapter
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, dataEventService, null, null, null, tiersDAO, null, null, null, audit);
		final EvenementCivilOptions options = new EvenementCivilOptions(true);
		final EvenementCivilInterne adapter = new Mariage(evtMariage, context, options);

		checkSetContent(Collections.<Long>emptySet(), dataEventService.getTiersChanged());
		checkSetContent(Collections.<Long>emptySet(), dataEventService.getDroitsChanged());
		checkSetContent(new HashSet<>(Arrays.asList(noIndMadame, noIndMonsieur)), dataEventService.getIndividusChanged());
	}

	private static void checkSetContent(Set<Long> expected, Set<Long> found) {
		if (expected == null) {
			Assert.assertNull(found);
		}
		else {
			Assert.assertNotNull(found);
			Assert.assertEquals(expected.size(), found.size());
			for (Long l : expected) {
				Assert.assertTrue("Elément " + l + " non trouvé dans le résultat", found.contains(l));
			}
			for (Long l : found) {
				Assert.assertTrue("Elément " + l + " trouvé en trop dans le résultat", expected.contains(l));
			}
		}
	}

	private static final class MyDataEventService implements DataEventService {

		private final Set<Long> tiersChanged = new HashSet<>();
		private final Set<Long> individusChanged = new HashSet<>();
		private final Set<Long> droitsChanged = new HashSet<>();

		@Override
		public void register(CivilDataEventListener listener) {
		}

		@Override
		public void unregister(CivilDataEventListener listener) {
		}

		@Override
		public void register(FiscalDataEventListener listener) {
		}

		@Override
		public void unregister(FiscalDataEventListener listener) {
		}

		@Override
		public void onTiersChange(long id) {
			tiersChanged.add(id);
		}

		@Override
		public void onIndividuChange(long id) {
			individusChanged.add(id);
		}

		@Override
		public void onEntrepriseChange(long id) {
		}

		@Override
		public void onDroitAccessChange(long id) {
			droitsChanged.add(id);
		}

		@Override
		public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		}

		@Override
		public void onImmeubleChange(long immeubleId) {
		}

		@Override
		public void onBatimentChange(long batimentId) {
		}

		@Override
		public void onCommunauteChange(long communauteId) {
		}

		@Override
		public void onLoadDatabase() {
		}

		@Override
		public void onTruncateDatabase() {
		}

		public Set<Long> getTiersChanged() {
			return tiersChanged;
		}

		public Set<Long> getIndividusChanged() {
			return individusChanged;
		}

		public Set<Long> getDroitsChanged() {
			return droitsChanged;
		}
	}
}
