package ch.vd.uniregctb.evenement.arrivee;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ArriveeAdapterTest extends WithoutSpringTest {

	// private static final Logger LOGGER =
	// Logger.getLogger(ArriveeAdapterTest.class);

	@Test
	public void testInitIndividuSeul() throws Exception {

		final long numeroIndividu = 12345L;
		final RegDate dateArrivee = RegDate.get(2002, 03, 15);
		final RegDate dateVeilleArrivee = dateArrivee.getOneDayBefore();

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(numeroIndividu);

		// Crée l'événement
		EvenementCivilRegroupe evenement = new EvenementCivilRegroupe(1L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateArrivee, numeroIndividu, habitant, 0L, null, 1234, null);

		// Prend le mock infrastructure par défaut
		ServiceInfrastructureService infrastructureService = new MockServiceInfrastructureService() {
			@Override
			protected void init() {
				// Pays
				pays.add(MockPays.Suisse);

				// Cantons
				cantons.add(MockCanton.Vaud);

				// Communes
				communesVaud.add(MockCommune.Lausanne);
				communesVaud.add(MockCommune.Cossonay);

				// Localités
				localites.add(MockLocalite.Lausanne);
				localites.add(MockLocalite.CossonayVille);

				// Rues
				rues.add(MockRue.CossonayVille.CheminDeRiondmorcel);
				rues.add(MockRue.Lausanne.AvenueDeBeaulieu);
			}
		};

		// Crée les données du mock service civil
		ServiceCivilService serviceCivil = new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(numeroIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(1980, 1, 1), dateVeilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(1980, 1, 1), dateVeilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille, dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille, dateArrivee, null);
			}
		};

		ArriveeAdapter adapter = new ArriveeAdapter();
		adapter.init(evenement, serviceCivil, infrastructureService);

		assertEquals(MockLocalite.Lausanne.getNomAbregeMinuscule(), adapter.getAncienneAdressePrincipale().getLocalite());
		assertEquals(MockCommune.Cossonay.getNomMinuscule(), adapter.getNouvelleCommunePrincipale().getNomMinuscule());
	}
}
