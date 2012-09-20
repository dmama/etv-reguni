package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
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

public class FinPermisTest extends WithoutSpringTest {

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa

	private static final RegDate DATE_DEBUT_PERMIS = RegDate.get(2007, 6, 1);
	private static final RegDate DATE_FIN_PERMIS = RegDate.get(2008, 10, 1);
	private static final RegDate DATE_OBTENTION_NATIONALITE = DATE_FIN_PERMIS;

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
			addNationalite(roberto, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null);
			addPermis(roberto, TypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), DATE_DEBUT_PERMIS.getOneDayBefore(), false);
			addPermis(roberto, TypePermis.ETABLISSEMENT, DATE_DEBUT_PERMIS, DATE_FIN_PERMIS, false);

			RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
			MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
			addDefaultAdressesTo(rosa);
			addOrigine(rosa, MockPays.Espagne.getNomMinuscule());
			addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);
			addPermis(rosa, TypePermis.COURTE_DUREE, RegDate.get(2003, 10, 25), DATE_DEBUT_PERMIS.getOneDayBefore(), false);
			addPermis(rosa, TypePermis.ETABLISSEMENT, DATE_DEBUT_PERMIS, DATE_FIN_PERMIS, false);
		}
	});

	final MockTiersDAO tiersDAO = new MockTiersDAO();
	private EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO);
	private EvenementCivilOptions options = new EvenementCivilOptions(false);

	@Test
	public void testPermisCAvecNationalite() throws Exception {
		PersonnePhysique roberto = new PersonnePhysique(true);
		roberto.setNumero(NUMERO_INDIVIDU);
		EvenementCivilRegPP evenementsCivils = new EvenementCivilRegPP(1L, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_FIN_PERMIS, NUMERO_INDIVIDU, 0L, 1234, null);
		FinPermis adapter = new FinPermis(evenementsCivils, context, options);
	}

	@Test
	public void testPermisCSansNationalite() throws Exception {
		PersonnePhysique rosa = new PersonnePhysique(true);
		rosa.setNumero(NUMERO_INDIVIDU_2);
		EvenementCivilRegPP evenementsCivils = new EvenementCivilRegPP(1L, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_FIN_PERMIS, NUMERO_INDIVIDU_2, 0L, 1234, null);
		FinPermis adapter = new FinPermis(evenementsCivils, context, options);
	}
}
