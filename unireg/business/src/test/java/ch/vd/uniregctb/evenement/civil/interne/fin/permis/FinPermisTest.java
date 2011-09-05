package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class FinPermisTest extends WithoutSpringTest {

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa

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
	ServiceCivilService serviceCivil = new DefaultMockServiceCivil() {

		@Override
		protected void init() {
			super.init();

			RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
			MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
			addDefaultAdressesTo(roberto);
			addOrigine(roberto, MockPays.Espagne, null, RegDate.get(1976, 1, 16));
			addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null);
			addNationalite(roberto, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null);
			addPermis(roberto, TypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), RegDate.get(2007, 5, 31), false);
			addPermis(roberto, TypePermis.ETABLISSEMENT, DATE_FIN_PERMIS.addYears(-5), DATE_FIN_PERMIS, false);

			RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
			MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
			addDefaultAdressesTo(rosa);
			addOrigine(rosa, MockPays.Espagne, null, dateNaissanceRosa);
			addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);
			addPermis(rosa, TypePermis.COURTE_DUREE, RegDate.get(2003, 10, 25), null, false);
			addPermis(rosa, TypePermis.ETABLISSEMENT, DATE_FIN_PERMIS.addYears(-5), DATE_FIN_PERMIS, false);
		}
	};

	private EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService);
	private EvenementCivilOptions options = new EvenementCivilOptions(false);

	@Test
	public void testPermisCAvecNationalite() throws Exception {
		PersonnePhysique roberto = new PersonnePhysique(true);
		roberto.setNumero(NUMERO_INDIVIDU);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_FIN_PERMIS, NUMERO_INDIVIDU, roberto, 0L, null, 1234, null);
		FinPermis adapter = new FinPermis(evenementsCivils, context, options);
	}

	@Test
	public void testPermisCSansNationalite() throws Exception {
		PersonnePhysique rosa = new PersonnePhysique(true);
		rosa.setNumero(NUMERO_INDIVIDU_2);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER,
				EtatEvenementCivil.A_TRAITER, DATE_FIN_PERMIS, NUMERO_INDIVIDU_2, rosa, 0L, null, 1234, null);
		FinPermis adapter = new FinPermis(evenementsCivils, context, options);
	}
}
