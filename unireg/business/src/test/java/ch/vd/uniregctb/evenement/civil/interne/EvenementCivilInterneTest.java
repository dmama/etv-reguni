package ch.vd.uniregctb.evenement.civil.interne;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveePrincipale;
import ch.vd.uniregctb.evenement.civil.interne.mariage.Mariage;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilInterneTest extends WithoutSpringTest {

	@Test
	public void testInitCouple() throws Exception {

		/*
		 * Création des données des services
		 */
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;

		final ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService());
		final ServiceCivilService serviceCivil = new MockServiceCivil() {
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
		};

		/*
		 * Création d'un événement civil composé de deux individus
		 */
		final RegDate dateEvenement = RegDate.get(2008, 3, 10);
		final EvenementCivilExterne evenementArriveeCouple = new EvenementCivilExterne(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE,
				EtatEvenementCivil.A_TRAITER, dateEvenement, noIndividuPrincipal,
				null, noIndividuConjoint, null, MockCommune.Lausanne.getNoOFSEtendu(), null);

		/*
		 * Création et initialisation de l'adapter
		 */
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService);
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
		Assert.assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), adapter.getNumeroOfsCommuneAnnonce().intValue());
	}

	@Test
	public void testInitAvecRefreshCacheIndividu() throws Exception {

		final long noIndMonsieur = 1L;
		final long noIndMadame = 2L;
		final RegDate dateMariage = RegDate.get(1985, 7, 11);

		final ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService());
		final ServiceCivilService serviceCivil = new MockServiceCivil(infrastructureService) {
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
		};
		final MyDataEventService dataEventService = new MyDataEventService();

		/*
		 * Création d'un événement civil de mariage
		 */
		final EvenementCivilExterne
				evtMariage = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER, dateMariage, noIndMonsieur, null, null, null, MockCommune.Lausanne.getNoOFSEtendu(), null);

		// passage dans l'init de l'adapter
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, dataEventService, null, null, null, null, null, null);
		final EvenementCivilOptions options = new EvenementCivilOptions(true);
		final EvenementCivilInterne adapter = new Mariage(evtMariage, context, options);

		checkSetContent(Collections.<Long>emptySet(), dataEventService.getTiersChanged());
		checkSetContent(Collections.<Long>emptySet(), dataEventService.getDroitsChanged());
		checkSetContent(new HashSet<Long>(Arrays.asList(noIndMadame, noIndMonsieur)), dataEventService.getIndividusChanged());
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

		private final Set<Long> tiersChanged = new HashSet<Long>();
		private final Set<Long> individusChanged = new HashSet<Long>();
		private final Set<Long> droitsChanged = new HashSet<Long>();

		@Override
		public void register(DataEventListener listener) {
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
		public void onPersonneMoraleChange(long id) {
		}

		@Override
		public void onDroitAccessChange(long ppId) {
			droitsChanged.add(ppId);
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
