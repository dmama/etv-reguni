package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
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
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Test de l'adapter de la tutelle :
 * ---------------------------------
 * - la recuperation du tuteur
 * - la recuperation du tuteur genéral
 * - la determination du type de tutelle
 *
 * @author Ludovic BERTIN
 *
 */
public class TutelleAdapterTest extends WithoutSpringTest {

	final static private RegDate DATE_TUTELLE = RegDate.get(2008, 01, 01);

	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR = 54321L;
	final static private long NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL = 45678L;
	final static private long NO_INDIVIDU_PUPILLE_SOUS_TUTELLE = 6789L;
	final static private long NO_INDIVIDU_PUPILLE_SOUS_CURATELLE = 23456L;
	final static private long NO_INDIVIDU_PUPILLE_SOUS_CONSEIL_LEGAL = 54321L;

	/**
	 * Teste la recuperation des tuteurs dans le cas d'un pupille avec tuteur individu.
	 * @throws Exception
	 */
	@Test
	public void testGetTuteurPupilleAvecTuteur() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.MESURE_TUTELLE, EtatEvenementCivil.A_TRAITER, DATE_TUTELLE, NO_INDIVIDU_PUPILLE_AVEC_TUTEUR , habitant, 0L, null, 1234, null);
		TutelleAdapter adapter = new  TutelleAdapter(evenement, context);
		Assert.notNull( adapter.getTuteur(), "le tuteur n'a pas pu être récupéré");
		Assert.isNull( adapter.getTuteurGeneral(), "le tuteur general ne devrait pas exister");
		Assert.isTrue(adapter.getTypeTutelle() == TypeTutelle.CONSEIL_LEGAL, "le type de tutelle n'a pas été correctement récupéré");
	}

	/**
	 * Teste la recuperation des tuteurs dans le cas d'un pupille avec tuteur general.
	 * @throws Exception
	 */
	@Test
	public void testGetTuteurPupilleAvecTuteurGeneral() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.MESURE_TUTELLE, EtatEvenementCivil.A_TRAITER, DATE_TUTELLE, NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL , habitant, 0L, null, 1234, null);
		TutelleAdapter adapter = new  TutelleAdapter(evenement, context);
		Assert.isNull( adapter.getTuteur(), "le tuteur ne devrait pas exister");
		Assert.notNull( adapter.getTuteurGeneral(), "le tuteur general n'a pas pu être récupéré");
		Assert.isTrue(adapter.getTypeTutelle() == TypeTutelle.TUTELLE, "le type de tutelle n'a pas été correctement récupéré");
	}

	/**
	 * Teste la recuperation du type de tutelle dans le cas d'un pupille sous tutelle.
	 * @throws Exception
	 */
	@Test
	public void testGetTypeTutellePupilleSousTutelle() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_PUPILLE_SOUS_TUTELLE);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.MESURE_TUTELLE, EtatEvenementCivil.A_TRAITER, DATE_TUTELLE, NO_INDIVIDU_PUPILLE_SOUS_TUTELLE , habitant, 0L, null, 1234, null);
		TutelleAdapter adapter = new  TutelleAdapter(evenement, context);
		Assert.isTrue(adapter.getTypeTutelle() == TypeTutelle.TUTELLE, "le type de tutelle n'a pas été correctement récupéré");
	}

	/**
	 * Teste la recuperation du type de tutelle dans le cas d'un pupille sous curatelle.
	 * @throws Exception
	 */
	@Test
	public void testGetTypeTutellePupilleSousCuratelle() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_PUPILLE_SOUS_CURATELLE);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.MESURE_TUTELLE, EtatEvenementCivil.A_TRAITER, DATE_TUTELLE, NO_INDIVIDU_PUPILLE_SOUS_CURATELLE , habitant, 0L, null, 1234, null);
		TutelleAdapter adapter = new  TutelleAdapter(evenement, context);
		Assert.isTrue(adapter.getTypeTutelle() == TypeTutelle.CURATELLE, "le type de tutelle n'a pas été correctement récupéré");
	}

	/**
	 * Teste la recuperation du type de tutelle dans le cas d'un pupille sous conseil legal.
	 * @throws Exception
	 */
	@Test
	public void testGetTypeTutellePupilleSousConseilLegal() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_PUPILLE_SOUS_CONSEIL_LEGAL);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.MESURE_TUTELLE, EtatEvenementCivil.A_TRAITER, DATE_TUTELLE, NO_INDIVIDU_PUPILLE_SOUS_CONSEIL_LEGAL , habitant, 0L, null, 1234, null);
		TutelleAdapter adapter = new  TutelleAdapter(evenement, context);
		Assert.isTrue(adapter.getTypeTutelle() == TypeTutelle.CONSEIL_LEGAL, "le type de tutelle n'a pas été correctement récupéré");
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
			MockIndividu momo = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu pierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
			MockIndividu bea = addIndividu(NO_INDIVIDU_PUPILLE_SOUS_CURATELLE, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
			MockIndividu julie = addIndividu(NO_INDIVIDU_PUPILLE_SOUS_TUTELLE, RegDate.get(1977, 4, 19), "Goux", "Julie", false);
			MockIndividu david = addIndividu(NO_INDIVIDU_PUPILLE_AVEC_TUTEUR_GENERAL, RegDate.get(1964, 1, 23), "Dagobert", "David", true);
			MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(pierre);
			addDefaultAdressesTo(bea);
			addDefaultAdressesTo(julie);
			addDefaultAdressesTo(david);
			addDefaultAdressesTo(julien);

			/* tutelles */
			setTutelle(momo, pierre, null, TypeTutelle.CONSEIL_LEGAL);
			setTutelle(bea, pierre, null, TypeTutelle.CURATELLE);
			setTutelle(julie, pierre, null, TypeTutelle.TUTELLE);
			setTutelle(david, null, TypeTutelle.TUTELLE);
		}
	};

	private EvenementCivilContext context = new EvenementCivilContext(serviceCivilSimple, infrastructureService, null, null, null, null, null, null, false);
}
