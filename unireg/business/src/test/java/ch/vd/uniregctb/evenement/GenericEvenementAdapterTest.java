package ch.vd.uniregctb.evenement;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class GenericEvenementAdapterTest extends WithoutSpringTest {

	@Test
	public void testInitCouple() throws Exception {

		/*
		 * Création des données des services
		 */
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;

		final ServiceInfrastructureService infrastructureService = new DefaultMockServiceInfrastructureService();
		final ServiceCivilService serviceCivil = new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				final RegDate dateArrivee = RegDate.get(1980, 1, 1);	// sans importance
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		};

		/*
		 * Création d'un événement civil composé de deux individus
		 */
		final RegDate dateEvenement = RegDate.get(2008, 03, 10);
		final EvenementCivilData evenementArriveeCouple = new EvenementCivilData(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE,
				EtatEvenementCivil.A_TRAITER, dateEvenement, noIndividuPrincipal,
				null, noIndividuConjoint, null, MockCommune.Lausanne.getNoOFSEtendu(), null);

		/*
		 * Création et initialisation de l'adapter
		 */
		final GenericEvenementAdapter adapter = new GenericEvenementAdapter() {
			// rien à faire ici, uniquement pour pouvoir instancier la classe
		};
		adapter.init(evenementArriveeCouple, serviceCivil, infrastructureService);

		/*
		 * Test de la méthode init dans ce cas
		 */
		assertEquals(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, adapter.getType());
		assertNotNull(adapter.getNoIndividu());
		assertEquals(noIndividuPrincipal, adapter.getNoIndividu().longValue());
		assertNotNull(adapter.getNoIndividuConjoint());
		assertEquals(noIndividuConjoint, adapter.getNoIndividuConjoint().longValue());
		assertEquals(dateEvenement, adapter.getDate());
		assertNotNull(adapter.getNumeroOfsCommuneAnnonce());
		assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), adapter.getNumeroOfsCommuneAnnonce().intValue());
	}
}
