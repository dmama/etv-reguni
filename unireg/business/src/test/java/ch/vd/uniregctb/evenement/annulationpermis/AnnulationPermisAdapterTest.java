package ch.vd.uniregctb.evenement.annulationpermis;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class AnnulationPermisAdapterTest extends WithoutSpringTest {

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa

	private static final RegDate DATE_OBTENTION_PERMIS = RegDate.get(2008, 10, 1);
	private static final RegDate DATE_ANNULATION_PERMIS = DATE_OBTENTION_PERMIS.addMonths(2);

	private MockPermis permisRoberto;
	private MockPermis permisRosa;

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
	ServiceCivilService serviceCivil = new DefaultMockServiceCivil() {

		@Override
		protected void init() {
			super.init();

			RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
			MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
			addDefaultAdressesTo(roberto);
			addOrigine(roberto, MockPays.Espagne, null, RegDate.get(1976, 1, 16));
			addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null, 0);
			addPermis(roberto, EnumTypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), RegDate.get(2007, 5, 31), 0, false);
			permisRoberto = (MockPermis) addPermis(roberto, EnumTypePermis.ETABLLISSEMENT, DATE_OBTENTION_PERMIS, null, 1, false);

			RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
			MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
			addDefaultAdressesTo(rosa);
			addOrigine(rosa, MockPays.Espagne, null, dateNaissanceRosa);
			addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null, 0);
			permisRosa = (MockPermis) addPermis(rosa, EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL, DATE_OBTENTION_PERMIS, null, 1, false);
		}
	};

	@Test
	public void testAnnulationPermisC() throws Exception {
		// Crée l'habitant
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NUMERO_INDIVIDU);
		// Simulation de l'annulation du permis dans le host
		permisRoberto.setDateAnnulation(DATE_ANNULATION_PERMIS);
		// Crée l'événement
		EvenementCivilRegroupe evenement = new EvenementCivilRegroupe(1L, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_PERMIS, NUMERO_INDIVIDU, habitant, 0L, null, 1234, null);
		// Teste l'adapter
		AnnulationPermisAdapter adapter = new AnnulationPermisAdapter();
		adapter.init(evenement, serviceCivil, infrastructureService);
		assertEquals(EnumTypePermis.ETABLLISSEMENT, adapter.getTypePermis());
	}

	@Test
	public void testPermisCSansNationalite() throws Exception {
		// Crée l'habitant
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NUMERO_INDIVIDU_2);
		// Simulation de l'annulation du permis dans le host
		permisRosa.setDateAnnulation(DATE_ANNULATION_PERMIS);
		// Crée l'événement
		EvenementCivilRegroupe evenement = new EvenementCivilRegroupe(1L, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_PERMIS, NUMERO_INDIVIDU_2, habitant, 0L, null, 1234, null);
		// Teste l'adapter
		AnnulationPermisAdapter adapter = new AnnulationPermisAdapter();
		adapter.init(evenement, serviceCivil, infrastructureService);
		assertEquals(EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL, adapter.getTypePermis());
	}

}
