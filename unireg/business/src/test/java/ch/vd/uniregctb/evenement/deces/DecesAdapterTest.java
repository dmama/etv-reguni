package ch.vd.uniregctb.evenement.deces;

import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Test de l'adapter du deces :
 * --------------------------
 * - la recuperation du conjoint survivant
 *
 * @author Ludovic BERTIN
 *
 */
public class DecesAdapterTest extends WithoutSpringTest {

	final static private RegDate DATE_DECES = RegDate.get(2008, 01, 01);

	/**
	 * Le numéro d'individu du défunt célibataire.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu du défunt marié seul.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE_SEUL = 12345L;

	/**
	 * Le numéro d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE = 54321L;

	/**
	 * Le numéro d'individu du veuf marié.
	 */
	private static final Long NO_INDIVIDU_VEUF_MARIE = 23456L;

	/**
	 * Le numéro d'individu du défunt pacsé.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_PACSE = 45678L;

	/**
	 * Le numéro d'individu du veuf pacsé.
	 */
	private static final Long NO_INDIVIDU_VEUF_PACSE = 56789L;


	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un célibataire.
	 * @throws Exception
	 */
	@Test
	public void testGetConjointSurvivantCelibataire() throws Exception {
		// Cas du célibataire
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
		EvenementCivilData evenement = new EvenementCivilData(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_CELIBATAIRE , habitant, 0L, null, 1234, null);
		DecesAdapter adapter = new DecesAdapter();
		adapter.init(evenement, serviceCivilSimple, infrastructureService, null);
		Assert.isNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un celibataire ne doit pas exister");
	}


	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un marié seul.
	 * @throws Exception
	 */
	@Test
	public void testGetConjointSurvivantMarieSeul() throws Exception {
		// Cas du marié seul
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_MARIE_SEUL);
		EvenementCivilData evenement = new EvenementCivilData(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_MARIE_SEUL , habitant, 0L, null, 1234, null);
		DecesAdapter adapter = new DecesAdapter();
		adapter.init(evenement, serviceCivilSimple, infrastructureService, null);
		Assert.isNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un marié seul ne doit pas exister");
	}

	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un marié en couple.
	 * @throws Exception
	 */
	@Test
	public void testGetConjointSurvivantMarieCouple() throws Exception {
		// Cas du marié
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_MARIE);
		EvenementCivilData evenement = new EvenementCivilData(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_MARIE , habitant, 0L, null, 1234, null);
		DecesAdapter adapter = new DecesAdapter();
		adapter.init(evenement, serviceCivilSimple, infrastructureService, null);
		Assert.notNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un marié doit exister");
		Assert.isTrue( adapter.getConjointSurvivant().getNoTechnique() == NO_INDIVIDU_VEUF_MARIE, "le conjoint survivant n'est pas celui attendu");
	}

	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un pacsé.
	 * @throws Exception
	 */
	@Test
	public void testGetConjointSurvivantPacse() throws Exception {
		// Cas du pacsé
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_PACSE);
		EvenementCivilData evenement = new EvenementCivilData(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_PACSE , habitant, 0L, null, 1234, null);
		DecesAdapter adapter = new DecesAdapter();
		adapter.init(evenement, serviceCivilSimple, infrastructureService, null);
		Assert.notNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un pacsé doit pas exister");
		Assert.isTrue( adapter.getConjointSurvivant().getNoTechnique() == NO_INDIVIDU_VEUF_PACSE, "le conjoint survivant n'est pas celui attendu");
	}

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
		}
	};

	// Crée les données du mock service civil
	ServiceCivilService serviceCivilSimple = new DefaultMockServiceCivil() {
		@Override
		protected void init() {
			MockIndividu momo = addIndividu(54321, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu pierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
			MockIndividu bea = addIndividu(23456, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
			MockIndividu julie = addIndividu(6789, RegDate.get(1977, 4, 19), "Goux", "Julie", false);
			MockIndividu david = addIndividu(45678, RegDate.get(1964, 1, 23), "Dagobert", "David", true);
			MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(pierre);
			addDefaultAdressesTo(bea);
			addDefaultAdressesTo(julie);
			addDefaultAdressesTo(david);
			addDefaultAdressesTo(julien);

			/* mariages, pacs */
			marieIndividus(momo, bea, RegDate.get(1986, 4, 8));
			marieIndividus(david, julien, RegDate.get(1986, 4, 8));
			marieIndividu(pierre, RegDate.get(1986, 4, 8));

			/* origines */
			addOrigine(bea, null, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
			addOrigine(julien, null, MockCommune.Lausanne, RegDate.get(1966, 11, 2));
		}
	};

	// Crée les données du mock service civil
	ServiceCivilService serviceCivilAvecDiversesOrigines = new DefaultMockServiceCivil() {
		@Override
		protected void init() {
			MockIndividu momo = addIndividu(54321, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu pierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
			MockIndividu bea = addIndividu(23456, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
			MockIndividu julie = addIndividu(6789, RegDate.get(1977, 4, 19), "Goux", "Julie", false);
			MockIndividu david = addIndividu(45678, RegDate.get(1964, 1, 23), "Dagobert", "David", true);
			MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);
			MockIndividu jc = addIndividu(45451, RegDate.get(1975, 4, 12), "Dus", "Jean-Claude", true);
			MockIndividu melanie = addIndividu(45452, RegDate.get(1977, 4, 19), "Zettofrais", "Melanie", false);
			MockIndividu simon = addIndividu(45453, RegDate.get(1975, 4, 12), "Dus", "Simon", true);
			MockIndividu simone = addIndividu(45454, RegDate.get(1977, 4, 19), "Zettofrais", "Simone", false);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(pierre);
			addDefaultAdressesTo(bea);
			addDefaultAdressesTo(julie);
			addDefaultAdressesTo(david);
			addDefaultAdressesTo(julien);
			addDefaultAdressesTo(jc);
			addDefaultAdressesTo(melanie);
			addDefaultAdressesTo(simon);
			addDefaultAdressesTo(simone);

			/* mariages, pacs */
			marieIndividus(momo, bea, RegDate.get(1986, 4, 8));
			marieIndividus(david, julien, RegDate.get(1986, 4, 8));
			marieIndividus(jc, melanie, RegDate.get(2004, 2, 18));
			marieIndividus(simon, simone, RegDate.get(2004, 2, 18));
			marieIndividu(pierre, RegDate.get(1986, 4, 8));

			/* origines */
			addOrigine(bea, null, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
			addOrigine(julien, MockPays.France, null, RegDate.get(1966, 11, 2));
			addOrigine(melanie, MockPays.France, null, RegDate.get(1977, 4, 19));
			addOrigine(simone, MockPays.France, null, RegDate.get(1977, 4, 19));

			/* permis */
			addPermis(julien, TypePermis.FRONTALIER, RegDate.get(1986, 11, 2), RegDate.get(1991, 11, 1), 0, false);
			addPermis(julien, TypePermis.ETABLISSEMENT, RegDate.get(1991, 11, 2), null, 1, false);
			addPermis(melanie, TypePermis.FRONTALIER, RegDate.get(1995, 11, 2), null, 0, false);
		}
	};

}
