package ch.vd.uniregctb.evenement.depart;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class DepartHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(DepartHandlerTest.class);

	private static final int NUMERO_INDIVIDU_SEUL = 1234;
	private static final int NO_IND_RAMONA = 1242;
	private static final int NO_IND_PAUL = 1243;
	private static final int NO_IND_ALBERT = 1244;
	private static final int NO_IND_ADRIEN = 1245;

	private static final RegDate DATE_EVENEMENT = RegDate.get(2008, 8, 19);
	private static final RegDate DATE_EVENEMENT_FIN_MOIS = RegDate.get(2008, 7, 26);
	private static final RegDate DATE_EVENEMENT_DEBUT_MOIS = RegDate.get(2008, 7, 10);
	private static final RegDate DATE_EVENEMENT_FIN_ANNEE = RegDate.get(2008, 12, 27);

	private static final RegDate DATE_ANTERIEURE_ADRESSE_ACTUELLE = RegDate.get(1940, 11, 19);

	private EvenementFiscalService evenementFiscalService;

	//JIRA 1996
	private final int noIndCharles = 782551;
	private final int noIndGeorgette = 782552;

	private MockIndividu indCharles;
	private MockIndividu indGorgette;

	private long noHabCharles;
	private long noHabGeorgette;
	private long noMenage;
	private final RegDate dateMariage = RegDate.get(1977, 1, 6);
	private final RegDate dateDebutForChamblon = RegDate.get(1981,2, 1);
	private final RegDate dateDepart = RegDate.get(2009, 8, 31);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.Chamblon;
	private final MockCommune communeArrivee = MockCommune.Enney;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "deparHC26012004.xml";

	//private EvenementCivilDAO evenementCivilDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		loadDatabase(DB_UNIT_DATA_FILE);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu marianne = addIndividu(1234, RegDate.get(1961, 3, 12), "Durant", "Marianne", false);
				final MockIndividu jhonny = addIndividu(1235, RegDate.get(1961, 3, 12), "Duretique", "Jhonny", true);
				final MockIndividu Lucien = addIndividu(1236, RegDate.get(1961, 3, 12), "muller", "Lucien", true);
				final MockIndividu Eva = addIndividu(1237, RegDate.get(1961, 3, 12), "muller", "Eva", false);
				final MockIndividu george = addIndividu(1238, RegDate.get(1961, 3, 12), "Durant", "George", false);
				final MockIndividu antoine = addIndividu(1239, RegDate.get(1961, 3, 12), "Duprés", "Antoine", true);
				final MockIndividu angela = addIndividu(1240, RegDate.get(1961, 3, 12), "kob", "Angela", false);
				final MockIndividu gege = addIndividu(1241, RegDate.get(1961, 3, 12), "Gégé", "Aglae", false);
				final MockIndividu ramona = addIndividu(NO_IND_RAMONA, RegDate.get(1961, 3, 12), "Ramona", "Cheminée", false);
				final MockIndividu paul = addIndividu(NO_IND_PAUL, RegDate.get(1961, 3, 12), "Ovent", "Paul", true);
				final MockIndividu albert = addIndividu(NO_IND_ALBERT, RegDate.get(1961, 3, 12), "Pittet", "Albert", true);

				setUpIndividuAdresseHC(marianne, RegDate.get(1980, 11, 2), DATE_EVENEMENT);
				setUpIndividuAdresseHC(jhonny, RegDate.get(1980, 11, 2), DATE_EVENEMENT);
				setUpIndividuAdresseHC(Lucien, RegDate.get(1980, 11, 2), DATE_EVENEMENT);
				setUpIndividuAdresseHC(Eva, RegDate.get(1980, 11, 2), DATE_EVENEMENT);

				setUpIndividuAdresseHS(george, RegDate.get(1980, 11, 2), DATE_EVENEMENT);
				setUpIndividuAdresseHC(antoine, RegDate.get(1980, 11, 2), DATE_EVENEMENT_FIN_MOIS);
				setUpIndividuAdresseHC(angela, RegDate.get(1980, 11, 2), DATE_EVENEMENT_DEBUT_MOIS);
				setUpIndividuAdresseHC(albert, RegDate.get(1980, 11, 2), DATE_EVENEMENT_FIN_ANNEE);

				marieIndividus(Lucien, Eva, RegDate.get(1986, 4, 8));
				marieIndividu(jhonny, RegDate.get(1986, 4, 8));

				setUpIndividuAdresseSecondaire(gege, RegDate.get(1980, 11, 2), DATE_EVENEMENT, true);
				setUpIndividuAdresseSecondaire(ramona, RegDate.get(1980, 11, 2), DATE_EVENEMENT, false);

				setUpIndividuNouvelleAdresseInconnue(paul, RegDate.get(1980, 11, 2), DATE_EVENEMENT);
				setUpJira1996();
				setUpJira2161();
			}

			protected void setUpIndividuAdresseHC(Individu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);
				// adresse courrier
				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.COURRIER);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);

				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateEvenement.getOneDayAfter());
				adresse.setLocalite(MockLocalite.Zurich.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Zurich.getNomMajuscule());
				adresse.setCasePostale("8001");
				adresse.setNumeroPostal("8001");
				adresse.setNumeroOrdrePostal(MockLocalite.Zurich.getNoOrdre());
				adresse.setNpa("8001");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Zurich);
				add(individu, adresse);
			}

			protected void setUpIndividuNouvelleAdresseInconnue(Individu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);
				// adresse courrier
				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.COURRIER);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);
			}

			protected void setUpIndividuAdresseSecondaire(Individu individu, RegDate dateDebut, RegDate dateEvenement, boolean residencePrincipaleHorsCanton) {

				final MockCommune cossonay = MockCommune.Cossonay;
				MockAdresse adresse = new MockAdresse();
				adresse.setCommuneAdresse(cossonay);
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);

				// adresse secondaire
				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.SECONDAIRE);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.Lausanne.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Lausanne.getNomMajuscule());
				adresse.setCasePostale("1004");
				adresse.setNumeroPostal("1004");
				adresse.setNumeroOrdrePostal(MockLocalite.Lausanne.getNoOrdre());
				adresse.setNpa("1004");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Lausanne);
				add(individu, adresse);

				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateEvenement.getOneDayAfter());
				if (residencePrincipaleHorsCanton) {
					adresse.setLocalite(MockLocalite.Zurich.getNomCompletMajuscule());
					adresse.setLieu(MockCommune.Zurich.getNomMajuscule());
					adresse.setCasePostale("8001");
					adresse.setNumeroPostal("8001");
					adresse.setNumeroOrdrePostal(MockLocalite.Zurich.getNoOrdre());
					adresse.setNpa("8001");
					adresse.setCommuneAdresse(MockCommune.Zurich);
				}
				else {
					adresse.setLocalite(MockLocalite.Vevey.getNomCompletMajuscule());
					adresse.setLieu(MockCommune.Vevey.getNomMajuscule());
					adresse.setCasePostale("1043");
					adresse.setNumeroPostal("1043");
					adresse.setNumeroOrdrePostal(MockLocalite.Vevey.getNoOrdre());
					adresse.setNpa("1043");
					adresse.setCommuneAdresse(MockCommune.Vevey);
				}
				adresse.setPays(MockPays.Suisse);
				add(individu, adresse);
			}

			protected void setUpIndividuAdresseHS(Individu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateDebut);
				adresse.setDateFinValidite(dateEvenement);
				adresse.setLocalite(MockLocalite.CossonayVille.getNomCompletMajuscule());
				adresse.setLieu(MockCommune.Cossonay.getNomMajuscule());
				adresse.setCasePostale("1304");
				adresse.setNumeroPostal("1304");
				adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
				adresse.setNpa("1304");
				adresse.setPays(MockPays.Suisse);
				adresse.setCommuneAdresse(MockCommune.Cossonay);
				add(individu, adresse);

				adresse = new MockAdresse();
				adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
				adresse.setDateDebutValidite(dateEvenement.getOneDayAfter());
				// adresse.setLocalite(MockLocalite.P);
				// adresse.setLieu(MockCommune.Zurich.getNomMajuscule());
				adresse.setCasePostale("31320");
				adresse.setNumeroPostal("31320");
				// adresse.setNumeroOrdrePostal(MockLocalite.Zurich.getNoOrdre());
				adresse.setNpa("31320");
				adresse.setPays(MockPays.France);
				add(individu, adresse);
			}

			protected void setUpJira1996() {

				final RegDate dateAmenagement = RegDate.get(1977, 1, 6);


				final RegDate dateNaissanceCharles = RegDate.get(1944, 8, 2);
				indCharles = addIndividu(noIndCharles, dateNaissanceCharles, "CHABOUDEZ", "Charles", true);
				addOrigine(indCharles, MockPays.Suisse, MockCommune.Neuchatel, dateNaissanceCharles);
				addNationalite(indCharles, MockPays.Suisse, dateNaissanceCharles, null, 1);
				addAdresse(indCharles, EnumTypeAdresse.PRINCIPALE, MockRue.Chamblon.GrandRue, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, EnumTypeAdresse.COURRIER,  MockRue.Chamblon.GrandRue, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, EnumTypeAdresse.PRINCIPALE, MockRue.Enney.chemin, null, dateArrivee, null);

				final RegDate dateNaissanceGeorgette = RegDate.get(1946, 5, 14);
				indGorgette = addIndividu(noIndGeorgette, dateNaissanceGeorgette, "CHABOUDEZ", "Georgette", false);
				addOrigine(indGorgette, MockPays.Suisse, MockCommune.Neuchatel, dateNaissanceGeorgette);
				addNationalite(indGorgette, MockPays.Suisse, dateNaissanceGeorgette, null, 1);
				addAdresse(indGorgette, EnumTypeAdresse.PRINCIPALE,  MockRue.Chamblon.GrandRue, null, dateAmenagement, null);
				addAdresse(indGorgette, EnumTypeAdresse.COURRIER,  MockRue.Chamblon.GrandRue, null, dateAmenagement, null);
				addAdresse(indGorgette, EnumTypeAdresse.PRINCIPALE, MockRue.Enney.chemin, null, dateArrivee, null);
				marieIndividus(indCharles, indGorgette, dateMariage);
			}

			private void setUpJira2161() {

				// le but sera de lancer un événement de départ secondaire en 2007, alors qu'aucune adresse secondaire n'est active
				final MockIndividu adrien = addIndividu(NO_IND_ADRIEN, RegDate.get(1956, 6, 1), "Nadire", "Adrien", true);
				addAdresse(adrien, EnumTypeAdresse.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, EnumTypeAdresse.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, EnumTypeAdresse.SECONDAIRE, MockRue.Bex.RouteDuBoet, null, RegDate.get(2006, 1, 20), RegDate.get(2006, 7, 23));
			}

		});
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				setUpForFiscal(1234L, 12346791, MockCommune.Cossonay.getNoOFS(), ModeImposition.SOURCE);
				setUpForFiscal(1239L, 12346792, MockCommune.Cossonay.getNoOFS(), ModeImposition.SOURCE);
				setUpForFiscal(1240L, 12346793, MockCommune.Cossonay.getNoOFS(), ModeImposition.SOURCE);
				// for fiscal principal sur residence secondaire
				setUpForFiscal(1241L, 12346794, MockCommune.Lausanne.getNoOFS(), ModeImposition.SOURCE);
				setUpForFiscal(NO_IND_RAMONA, 12346795, MockCommune.Lausanne.getNoOFS(), ModeImposition.SOURCE);
				setUpForFiscal(NO_IND_PAUL, 12346796, MockCommune.Cossonay.getNoOFS(), ModeImposition.SOURCE);
				setUpForFiscal(NO_IND_ALBERT, 12346797, MockCommune.Cossonay.getNoOFS(), ModeImposition.ORDINAIRE);
				return null;
			}
		});
	}

	/**
	 * Permet de tester le JIRA 1996
	 *
	 */
	@Test
	public void testHandleJira1996() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		MockDepart departCharles = createValidDepart(noIndCharles, dateDepart, true);
		MockDepart departGeorgette = createValidDepart(noIndGeorgette, dateDepart, true);

		handleDepartSimple(departCharles);
		handleDepartSimple(departGeorgette);
		Tiers tiers = tiersDAO.get(26012004L);
		ForFiscalPrincipal forFiscalPrincipalOuvert = tiers.getForFiscalPrincipalAt(dateArrivee);

		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalOuvert.getTypeAutoriteFiscale());
		Assert.assertTrue(communeArrivee.getNoOFS() == forFiscalPrincipalOuvert.getNumeroOfsAutoriteFiscale());



		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(tiers);
		Assert.assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", MockDepart.findEvenementFermetureFor(lesEvenements, departCharles));

	}

	@Test
	public void testHandleJira2161() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ secondaire alors qu'aucune adresse secondaire n'est valide");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(NO_IND_ADRIEN);
				return null;
			}
		});
		
		final MockDepart depart = createValidDepart(NO_IND_ADRIEN, RegDate.get(2007, 6, 30), false);

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		handleDepart(depart, erreurs, warnings);

		Assert.assertEquals(1, erreurs.size());

		final EvenementCivilErreur erreur = erreurs.get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals("Adresse de résidence avant départ inconnue", erreur.getMessage());
	}

	/**
	 * Teste la complétude du départ d'un individu seul.
	 */
	@Test
	public void testCheckCompletenessIndividuSeul() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		LOGGER.debug("Test départ individu seul...");
		MockDepart depart = createValidDepart(1234, DATE_EVENEMENT, true);
		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		Assert.assertTrue("individu célibataire : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	public void testCheckCompletenessIndividuMarieSeul() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		LOGGER.debug("Test départ individu marié seul...");

		MockDepart depart = createValidDepart(1235, DATE_EVENEMENT, true);
		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		Assert.assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu marié seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	public void testCheckCompletenessIndividuMarie() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		LOGGER.debug("Test départ individu marié ...");

		MockDepart depart = createValidDepart(1236, DATE_EVENEMENT, true);
		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		Assert.assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu marié  : OK");
	}

	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	private MockDepart createValidDepart(long noIndividu, RegDate dateEvenement, boolean principale) throws Exception {

		final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndividu, 0);

		final MockDepart depart = new MockDepart();
		if (principale) {
			depart.setType(TypeEvenementCivil.DEPART_COMMUNE);
		}
		else {
			depart.setType(TypeEvenementCivil.DEPART_SECONDAIRE);
		}

		depart.setIndividu(individu);

		// Adresse actuelle
		final AdressesCiviles adresseVaud = serviceCivil.getAdresses(noIndividu, dateEvenement, false);

		final MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		if (principale) {
			// Initialisation d'une date de fin de validité pour la résidence principale
			adressePrincipale.setDateFinValidite(dateEvenement);
		}

		// Adresse principale
		depart.setAncienneAdressePrincipale(adressePrincipale);
		depart.setAncienneAdresseCourrier(adresseVaud.courrier);

		// Commune dans vd
		final MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale);
		depart.setAncienneCommunePrincipale(communeVd);

		// Nouvelles adresses
		final AdressesCiviles adresseHorsVaud = serviceCivil.getAdresses(noIndividu, dateEvenement.getOneDayAfter(), false);

		final MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;
		depart.setAdressePrincipale(nouvelleAdresse);

		// Commune hors vd
		final MockCommune communeHorsVd = (MockCommune) serviceInfra.getCommuneByAdresse(nouvelleAdresse);
		depart.setNouvelleCommunePrincipale(communeHorsVd);
		depart.setNumeroOfsCommuneAnnonce(communeVd.getNoOFS());
		depart.setDate(dateEvenement);

		// En cas de depart d'une residence secondaire
		if (!principale && adresseVaud.secondaire != null) {
			MockAdresse adresseSecondaire = (MockAdresse) adresseVaud.secondaire;
			depart.setAncienneAdresseSecondaire(adresseSecondaire);
			MockCommune communeSecondaire = (MockCommune) serviceInfra.getCommuneByAdresse(adresseSecondaire);
			depart.setAncienneCommuneSecondaire(communeSecondaire);
			depart.setNumeroOfsCommuneAnnonce(communeSecondaire.getNoOFS());
		}

		return depart;
	}

	public void setUpForFiscal(long noIndividu, int numHabitant, int numOfs, ModeImposition modeImposition) {
		// Crée un habitant

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant.setNumero(new Long(numHabitant));
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		addForPrincipal(habitant, RegDate.get(1980, 1, 1), numOfs, modeImposition);
	}

	/**
	 * Teste la validation d'un départ antérieur à la date de fin de validité de l'adresse principale.
	 */
	@Test
	public void testValidateDepartAnterieurPrincipale() throws Exception {
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle...");
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		// initialisation de la date de fin de validité
		MockDepart depart = createValidDepart(1234, DATE_EVENEMENT, true);

		erreurs.clear();
		warnings.clear();

		depart.setDate(DATE_ANTERIEURE_ADRESSE_ACTUELLE);
		evenementCivilHandler.validate(depart, erreurs, warnings);

		Assert.assertTrue("Le départ est antérieur à la date de fin de validité de l'adresse actuelle, une erreur aurait du être déclenchée", findMessage(erreurs, "La date de départ est différente"));
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle : OK");

	}

	/**
	 * Teste de validation que la nouvelle commune principale n'est pas dans le canton de vaud
	 */
	@Test
	public void testValidateNouvelleCommunePrinHorsCanton() throws Exception {

		LOGGER.debug("Test si la nouvelle commune principale est hors canton...");
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		MockDepart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true);
		erreurs.clear();
		warnings.clear();

		final MockCommune nouvelleCommune = MockCommune.Cossonay;
		depart.setNouvelleCommunePrincipale(nouvelleCommune);

		evenementCivilHandler.validate(depart, erreurs, warnings);
		Assert.assertTrue("La nouvelle commune est dans le canton de Vaud, une erreur aurait du être déclenchée", findMessage(erreurs, "est toujours dans le canton de Vaud"));
		LOGGER.debug("Test nouvelle commune hors canton : OK");

	}

	/**
	 * Teste de validation de la commune d'annonce
	 */
	@Test
	public void testValidateCommuneAnnoncePrincipal() throws Exception {

		LOGGER.debug("Teste si la commune d'annonce principale est correcte...");
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		MockDepart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true);
		erreurs.clear();
		warnings.clear();
		depart.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());
		evenementCivilHandler.validate(depart, erreurs, warnings);
		Assert.assertTrue("La commune d'anonce et differente de celle de la dernière adresse, une erreur aurait du être déclenchée", findMessage(erreurs, "La commune d'annonce"));
		LOGGER.debug("Test commune d'annonce principale : OK");

	}

	/**
	 * Permet de tester le handle sur une personne seule
	 *
	 */
	@Test
	public void testHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		MockDepart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", MockDepart.findEvenementFermetureFor(lesEvenements, depart));

	}
	/**
	 * Permet de tester le handle sur une personne seule avec une nouvelle adresse inconnue
	 *
	 */
	@Test
	public void testHandleNouvelleAdresseInconnue() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		MockDepart depart = createValidDepart(NO_IND_PAUL, DATE_EVENEMENT, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", MockDepart.findEvenementFermetureFor(lesEvenements, depart));
	}

	@Test
	public void testHandleDepartHCFinAnnee() throws Exception {
		final MockDepart depart = createValidDepart(NO_IND_ALBERT, DATE_EVENEMENT_FIN_ANNEE, true);
		final ForFiscalPrincipal ffp = handleDepart(depart);
		Assert.assertEquals("Le for HC aurait dû être ouvert encore l'année du départ", DATE_EVENEMENT_FIN_ANNEE.getOneDayAfter(), ffp.getDateDebut());
	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale située hors canton
	 */
	@Test
	public void testHandleDepartSecondaireHorsCanton() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ d'une residence secondaire vaudoise.");

		MockDepart depart = createValidDepart(1241, DATE_EVENEMENT, false);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalFerme.getMotifFermeture());

		Assert.assertNotNull(forFiscalPrincipal.getMotifOuverture());
		Assert.assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		Assert.assertEquals(Integer.valueOf(MockCommune.Zurich.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", MockDepart.findEvenementFermetureFor(lesEvenements, depart));

	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale elle-même située dans le canton
	 */
	@Test
	public void testHandleDepartSecondaireVaudois() throws Exception {

		MockDepart depart = createValidDepart(NO_IND_RAMONA, DATE_EVENEMENT, false);

		final ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		final PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalFerme.getMotifFermeture());

		Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipal.getMotifOuverture());
		Assert.assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		Assert.assertEquals(Integer.valueOf(MockCommune.Vevey.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.assertFalse("Absence d'evenement de type femeture de for", MockDepart.findEvenementFermetureFor(lesEvenements, depart));
	}

	/**
	 * Permet de chercher la presence d'un message d'erreur dans la liste des erreurs de type evenement civil
	 *
	 * @param erreurs
	 * @param message
	 * @return
	 */
	private boolean findMessage(List<EvenementCivilErreur> erreurs, String message) {
		boolean isPresent = false;
		for (EvenementCivilErreur evenementErreur : erreurs) {
			if (evenementErreur.getMessage().contains(message)) {
				isPresent = true;
				break;
			}

		}

		return isPresent;
	}

	private ForFiscalPrincipal addForPrincipal(Contribuable tiers, RegDate ouverture, Integer noOFS, ModeImposition modeImposition) {
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(MotifFor.ARRIVEE_HC);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(modeImposition);
		f = (ForFiscalPrincipal) tiersService.addAndSave(tiers, f);
		return f;
	}

	/**
	 * En cas de départ dans un autre canton ou à l’étranger, si la date de l’événement survient après le 25 du mois et que le mode
	 * d’imposition est l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du mois
	 *
	 */
	@Test
	public void testDateDeFermetureFinDeMois() throws Exception {

		MockDepart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		Assert.assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		Assert.assertEquals("La date de fermeture est incorrecte", dateAttendu, forFiscalPrincipalFerme.getDateFin());
	}

	/**
	 * En cas de départ dans le canton de Neuchâtel avec l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du
	 * mois précédent l’événement si la date de l’événement est située entre le 1er et le 15 du mois, ces deux dates comprises.
	 */
	@Test
	public void testDateDeFermetureNeuchatelDebutMois() throws Exception {

		LOGGER.debug("Test de date de fermeture pour un depart vers Neuchatel entre le 1 et 15.");

		MockDepart depart = createValidDepart(1240, DATE_EVENEMENT_DEBUT_MOIS, true);
		depart.setNouvelleCommunePrincipale(MockCommune.Neuchatel);
		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		Assert.assertNotNull(forFiscalPrincipal);
		RegDate dateAttendu = RegDate.get(2008, 6, 30);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		Assert.assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + " " + dateAttendu, forFiscalPrincipalFerme.getDateFin().equals(dateAttendu));
		LOGGER.debug("Test de date de fermeture pour un depart vers Neuchatel entre le 1 et 15 OK");
	}

	/**
	 * En cas de départ dans le canton de Neuchâtel avec l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du
	 * mois de l’événement si la date de l’événement est située après le 15 du mois.
	 */
	@Test
	public void testDateDeFermetureNeuchatelFinMois() throws Exception {

		LOGGER.debug("Test de date de fermeture pour un depart vers Neuchatel après le 15.");

		MockDepart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, true);
		depart.setNouvelleCommunePrincipale(MockCommune.Neuchatel);
		RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		Assert.assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		Assert.assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + "" + dateAttendu, forFiscalPrincipalFerme.getDateFin().equals(dateAttendu));
		LOGGER.debug("Test de date de fermeture pour un depart vers Neuchatel après le 15 OK");
	}

	/**
	 * [UNIREG-1921] En cas de départ secondaire de la commune où se situe également le for principal, rien à faire sur les fors
	 */
	@Test
	public void testDepartSecondaireDeCommuneAvecForPrincipal() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, EnumTypeAdresse.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				addAdresse(ind, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 1, 1), dateDepart.getOneDayBefore());
				addAdresse(ind, EnumTypeAdresse.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateDepart, null);
			}
		});

		// mise en place des fors
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, dateDepart.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
				addForPrincipal(pp, dateDepart, MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				return null;
			}
		});

		// résumons-nous :
		// 1. l'événement de déménagement de Bussigny à Echallens est déjà arrivé
		// 2. nous allons maintenant recevoir un événement de départ secondaire depuis Echallens
		// 3. il ne faudrait pas créer un deuxième for principal sur Echallens (cas de UNIREG-1921)
		final Depart depart = createValidDepart(noIndividu, dateDepart, false);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(2, ff.size());

		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		Assert.assertNotNull(ffp);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(dateDepart, ffp.getDateDebut());
	}

	/**
	 * [UNIREG-1921] En cas de départ secondaire d'une commune vaudoise alors que l'adresse de domicile est hors-canton,
	 * le for vaudois doit se fermer...
	 */
	@Test
	public void testDepartSecondaireAvecResidencePrincipaleHC() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, EnumTypeAdresse.COURRIER, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
				addAdresse(ind, EnumTypeAdresse.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				addAdresse(ind, EnumTypeAdresse.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		// mise en place des fors
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				return null;
			}
		});

		// résumons-nous :
		// 1. nous allons maintenant recevoir un événement de départ secondaire depuis Echallens
		// 2. il faudrait que le for principal soit fermé et passe à Genève
		final Depart depart = createValidDepart(noIndividu, dateDepart, false);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(2, ff.size());

		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		Assert.assertNotNull(ffp);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Geneve.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
		Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
	}

	/**
	 * vérifie et traite un depart
	 *
	 * @param depart
	 * @return le fort fiscal après le traitement du départ
	 */
	private ForFiscalPrincipal handleDepart(Depart depart) {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		handleDepart(depart, erreurs, warnings);

		Assert.assertTrue("Une erreur est survenue lors du traitement du départ", erreurs.isEmpty());

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		Assert.assertNotNull(tiers);

		ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(depart.getDate().getOneDayAfter());
		Assert.assertNotNull("Le contribuable n'a aucun for fiscal", forFiscalPrincipal);

		return forFiscalPrincipal;
	}

	/**
	 * vérifie et traite un depart
	 *
	 * @param depart
	 * @return le fort fiscal après le traitement du départ
	 */
	private void handleDepartSimple(Depart depart) {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		handleDepart(depart, erreurs, warnings);

		if (!erreurs.isEmpty()) {
			for (EvenementCivilErreur erreur : erreurs) {
				LOGGER.error("Erreur trouvée : " + erreur.getMessage());
			}
			Assert.fail("Une erreur est survenue lors du traitement du départ");
		}
	}

	private void handleDepart(Depart depart, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		evenementCivilHandler.validate(depart, erreurs, warnings);
		evenementCivilHandler.handle(depart, warnings);
	}
}
