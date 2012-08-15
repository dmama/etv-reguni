package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.service.ServiceCivilImpl;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.MockTiersDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

import static junit.framework.Assert.assertEquals;

public class AnnulationPermisTest extends WithoutSpringTest {

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa

	private static final RegDate DATE_OBTENTION_PERMIS = RegDate.get(2008, 10, 1);
	private static final RegDate DATE_ANNULATION_PERMIS = DATE_OBTENTION_PERMIS.addMonths(2);

	private MockPermis permisRoberto;
	private MockPermis permisRosa;

	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new MockServiceInfrastructureService() {
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
	});

	// Crée les données du mock service civil
	ServiceCivilService serviceCivil = new ServiceCivilImpl(infrastructureService, new DefaultMockServiceCivil() {

		@Override
		protected void init() {
			super.init();

			RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
			MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
			addDefaultAdressesTo(roberto);
			addOrigine(roberto, MockPays.Espagne.getNomMinuscule());
			addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null);
			addPermis(roberto, TypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), RegDate.get(2007, 5, 31), false);
			permisRoberto = (MockPermis) addPermis(roberto, TypePermis.ETABLISSEMENT, DATE_OBTENTION_PERMIS, null, false);

			RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
			MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
			addDefaultAdressesTo(rosa);
			addOrigine(rosa, MockPays.Espagne.getNomMinuscule());
			addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);
			permisRosa = (MockPermis) addPermis(rosa, TypePermis.CONJOINT_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL, DATE_OBTENTION_PERMIS, null, false);
		}
	});

	final MockTiersDAO tiersDAO = new MockTiersDAO();
	final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO);
	final EvenementCivilOptions options = new EvenementCivilOptions(false);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();    //To change body of overridden methods use File | Settings | File Templates.
		tiersDAO.clear();
	}

	@Test
	public void testAnnulationPermisC() throws Exception {
		// Crée l'habitant
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(12345678L);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		// Simulation de l'annulation du permis dans le host
		permisRoberto.setDateAnnulation(DATE_ANNULATION_PERMIS);
		// Crée l'événement
		EvenementCivilRegPP evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_PERMIS, NUMERO_INDIVIDU, 0L, 1234, null);
		// Teste l'adapter
		AnnulationPermis adapter = new AnnulationPermis(evenement, context, options);
		assertEquals(TypePermis.ETABLISSEMENT, adapter.getTypePermis());
	}

	@Test
	public void testPermisCSansNationalite() throws Exception {
		// Crée l'habitant
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(12345678L);
		habitant.setNumero(NUMERO_INDIVIDU_2);
		// Simulation de l'annulation du permis dans le host
		permisRosa.setDateAnnulation(DATE_ANNULATION_PERMIS);
		// Crée l'événement
		EvenementCivilRegPP evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_PERMIS, NUMERO_INDIVIDU_2, 0L, 1234, null);
		// Teste l'adapter
		AnnulationPermis adapter = new AnnulationPermis(evenement, context, options);
		assertEquals(TypePermis.CONJOINT_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL, adapter.getTypePermis());
	}

}
