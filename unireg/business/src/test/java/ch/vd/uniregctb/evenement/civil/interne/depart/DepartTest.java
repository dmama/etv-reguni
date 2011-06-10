package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
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
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class DepartTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(DepartTest.class);

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

	private final RegDate dateMariage = RegDate.get(1977, 1, 6);
	private final RegDate dateDepart = RegDate.get(2009, 8, 31);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeArrivee = MockCommune.Enney;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "deparHC26012004.xml";

	//private EvenementCivilExterneDAO evenementCivilDAO;

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

			protected void setUpIndividuAdresseHC(MockIndividu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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
				adresse.setTypeAdresse(TypeAdresseCivil.COURRIER);
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
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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

			protected void setUpIndividuNouvelleAdresseInconnue(MockIndividu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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
				adresse.setTypeAdresse(TypeAdresseCivil.COURRIER);
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

			protected void setUpIndividuAdresseSecondaire(MockIndividu individu, RegDate dateDebut, RegDate dateEvenement, boolean residencePrincipaleHorsCanton) {

				final MockCommune cossonay = MockCommune.Cossonay;
				MockAdresse adresse = new MockAdresse();
				adresse.setCommuneAdresse(cossonay);
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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
				adresse.setTypeAdresse(TypeAdresseCivil.SECONDAIRE);
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
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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

			protected void setUpIndividuAdresseHS(MockIndividu individu, RegDate dateDebut, RegDate dateEvenement) {

				MockAdresse adresse = new MockAdresse();
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
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
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Enney.chemin, null, dateArrivee, null);

				final RegDate dateNaissanceGeorgette = RegDate.get(1946, 5, 14);
				indGorgette = addIndividu(noIndGeorgette, dateNaissanceGeorgette, "CHABOUDEZ", "Georgette", false);
				addOrigine(indGorgette, MockPays.Suisse, MockCommune.Neuchatel, dateNaissanceGeorgette);
				addNationalite(indGorgette, MockPays.Suisse, dateNaissanceGeorgette, null, 1);
				addAdresse(indGorgette, TypeAdresseCivil.PRINCIPALE,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indGorgette, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indGorgette, TypeAdresseCivil.PRINCIPALE, MockRue.Enney.chemin, null, dateArrivee, null);
				marieIndividus(indCharles, indGorgette, dateMariage);
			}

			private void setUpJira2161() {

				// le but sera de lancer un événement de départ secondaire en 2007, alors qu'aucune adresse secondaire n'est active
				final MockIndividu adrien = addIndividu(NO_IND_ADRIEN, RegDate.get(1956, 6, 1), "Nadire", "Adrien", true);
				addAdresse(adrien, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, TypeAdresseCivil.SECONDAIRE, MockRue.Bex.RouteDuBoet, null, RegDate.get(2006, 1, 20), RegDate.get(2006, 7, 23));
			}

		});
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		doInNewTransaction(new TxCallback<Object>() {
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

		Depart departCharles = createValidDepart(noIndCharles, dateDepart, true, null);
		Depart departGeorgette = createValidDepart(noIndGeorgette, dateDepart, true, null);

		handleDepartSimple(departCharles);
		handleDepartSimple(departGeorgette);
		Tiers tiers = tiersDAO.get(26012004L);
		ForFiscalPrincipal forFiscalPrincipalOuvert = tiers.getForFiscalPrincipalAt(dateArrivee);

		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalOuvert.getTypeAutoriteFiscale());
		Assert.assertTrue(communeArrivee.getNoOFS() == forFiscalPrincipalOuvert.getNumeroOfsAutoriteFiscale());



		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(tiers);
		Assert.assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		Assert.assertTrue("Absence d'événement de type femeture de for", findEvenementFermetureFor(lesEvenements, departCharles));

	}

	@Test
	public void testHandleJira2161() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ secondaire alors qu'aucune adresse secondaire n'est valide");

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(NO_IND_ADRIEN);
				return null;
			}
		});
		
		final Depart depart = createValidDepart(NO_IND_ADRIEN, RegDate.get(2007, 6, 30), false, null);

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		handleDepart(depart, erreurs, warnings);

		Assert.assertEquals(1, erreurs.size());

		final EvenementCivilExterneErreur erreur = erreurs.get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals("Adresse de résidence avant départ inconnue", erreur.getMessage());
	}

	/**
	 * Teste la complétude du départ d'un individu seul.
	 */
	@Test
	public void testCheckCompletenessIndividuSeul() throws Exception {

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		LOGGER.debug("Test départ individu seul...");
		Depart depart = createValidDepart(1234, DATE_EVENEMENT, true, null);
		depart.checkCompleteness(erreurs, warnings);
		Assert.assertTrue("individu célibataire : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	public void testCheckCompletenessIndividuMarieSeul() throws Exception {

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		LOGGER.debug("Test départ individu marié seul...");

		Depart depart = createValidDepart(1235, DATE_EVENEMENT, true, null);
		depart.checkCompleteness(erreurs, warnings);
		Assert.assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu marié seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	public void testCheckCompletenessIndividuMarie() throws Exception {

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		LOGGER.debug("Test départ individu marié ...");

		Depart depart = createValidDepart(1236, DATE_EVENEMENT, true, null);
		depart.checkCompleteness(erreurs, warnings);
		Assert.assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", erreurs.isEmpty());
		LOGGER.debug("Test départ individu marié  : OK");
	}

	private Depart createValidDepart(long noIndividu, RegDate dateEvenement, boolean principale, Integer overrideNoOfsCommuneAnnonce) throws Exception {

		final Individu individu = serviceCivil.getIndividu(noIndividu, 0);

		// Adresse actuelle
		final AdressesCiviles adresseVaud = new AdressesCiviles(serviceCivil.getAdresses(noIndividu, dateEvenement, false));
		final MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		if (principale && adressePrincipale != null) {
			// Initialisation d'une date de fin de validité pour la résidence principale
			adressePrincipale.setDateFinValidite(dateEvenement);
		}

		// Ancienne commune
		final MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale, dateEvenement);
		int noOFS;
		if (communeVd != null) {
			noOFS = communeVd.getNoOFS();
		}
		else {
			// j'ai mis "Croy", j'aurais pu mettre autre chose...
			noOFS = MockCommune.Croy.getNoOFSEtendu();
		}


		// Nouvelles adresses
		final AdressesCiviles adresseHorsVaud = new AdressesCiviles(serviceCivil.getAdresses(noIndividu, dateEvenement.getOneDayAfter(), false));
		final MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;

		// Nouvelle commune
		final MockCommune communeHorsVd = (MockCommune) serviceInfra.getCommuneByAdresse(nouvelleAdresse, dateEvenement.getOneDayAfter());

		// En cas de depart d'une residence secondaire
		final MockCommune communeSecondaire ;
		final MockAdresse adresseSecondaire;
		if (!principale && adresseVaud.secondaire != null) {
			adresseSecondaire = (MockAdresse) adresseVaud.secondaire;
			communeSecondaire = (MockCommune) serviceInfra.getCommuneByAdresse(adresseSecondaire, dateEvenement.getOneDayAfter());
			noOFS = communeSecondaire.getNoOFS();
		}
		else {
			communeSecondaire = null;
			adresseSecondaire = null;
		}

		if (overrideNoOfsCommuneAnnonce != null) {
			noOFS = overrideNoOfsCommuneAnnonce;
		}

		return new Depart(individu, null, dateEvenement, noOFS, communeVd, communeHorsVd, adressePrincipale, nouvelleAdresse, adresseVaud.courrier, null, communeSecondaire, adresseSecondaire,
				principale, context);
	}

	private Depart createValidDepart(long noIndividu, RegDate dateEvenement, MockCommune nouvelleCommune) throws Exception {

		final Individu individu = serviceCivil.getIndividu(noIndividu, 0);

		// Adresse actuelle
		final AdressesCiviles adresseVaud = new AdressesCiviles(serviceCivil.getAdresses(noIndividu, dateEvenement, false));
		final MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		if (adressePrincipale != null) {
			// Initialisation d'une date de fin de validité pour la résidence principale
			adressePrincipale.setDateFinValidite(dateEvenement);
		}

		// Ancienne commune
		final MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale, dateEvenement);
		final int noOFS = communeVd.getNoOFS();


		// Nouvelles adresses
		final AdressesCiviles adresseHorsVaud = new AdressesCiviles(serviceCivil.getAdresses(noIndividu, dateEvenement.getOneDayAfter(), false));
		final MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;

		// En cas de depart d'une residence secondaire


		return new Depart(individu, null, dateEvenement, noOFS, communeVd, nouvelleCommune, adressePrincipale, nouvelleAdresse, adresseVaud.courrier, null, null, null,
				true, context);
	}

	public void setUpForFiscal(long noIndividu, int numHabitant, int numOfs, ModeImposition modeImposition) {
		// Crée un habitant

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant.setNumero((long) numHabitant);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		addForPrincipal(habitant, RegDate.get(1980, 1, 1), numOfs, modeImposition);
	}

	/**
	 * Teste la validation d'un départ antérieur à la date de fin de validité de l'adresse principale.
	 */
	@Test
	public void testValidateDepartAnterieurPrincipale() throws Exception {
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle...");

		// mise-en-place des données à DATE_EVENEMENT
		final Individu individu = serviceCivil.getIndividu((long) 1234, 0);
		final AdressesCiviles adresseVaud = new AdressesCiviles(serviceCivil.getAdresses((long) 1234, DATE_EVENEMENT, false));
		final MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		adressePrincipale.setDateFinValidite(DATE_EVENEMENT);

		final MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale, DATE_EVENEMENT);
		int noOFS = communeVd.getNoOFS();
		final AdressesCiviles adresseHorsVaud = new AdressesCiviles(serviceCivil.getAdresses((long) 1234, DATE_EVENEMENT.getOneDayAfter(), false));
		final MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;
		final MockCommune communeHorsVd = (MockCommune) serviceInfra.getCommuneByAdresse(nouvelleAdresse, DATE_EVENEMENT.getOneDayAfter());

		// création d'un événement à DATE_ANTERIEURE_ADRESSE_ACTUELLE
		final Depart depart =
				new Depart(individu, null, DATE_ANTERIEURE_ADRESSE_ACTUELLE, noOFS, communeVd, communeHorsVd, adressePrincipale, nouvelleAdresse, adresseVaud.courrier, null, null, null, true,
						context);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		depart.validate(erreurs, warnings);

		Assert.assertTrue("Le départ est antérieur à la date de fin de validité de l'adresse actuelle, une erreur aurait du être déclenchée", findMessage(erreurs, "La date de départ est différente"));
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle : OK");
	}

	/**
	 * Teste de validation que la nouvelle commune principale n'est pas dans le canton de vaud
	 */
	@Test
	public void testValidateNouvelleCommunePrinHorsCanton() throws Exception {

		LOGGER.debug("Test si la nouvelle commune principale est hors canton...");
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, MockCommune.Cossonay);
		erreurs.clear();
		warnings.clear();

		depart.validate(erreurs, warnings);
		Assert.assertTrue("La nouvelle commune est dans le canton de Vaud, une erreur aurait du être déclenchée", findMessage(erreurs, "est toujours dans le canton de Vaud"));
		LOGGER.debug("Test nouvelle commune hors canton : OK");

	}

	/**
	 * Teste de validation de la commune d'annonce
	 */
	@Test
	public void testValidateCommuneAnnoncePrincipal() throws Exception {

		LOGGER.debug("Teste si la commune d'annonce principale est correcte...");
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true, MockCommune.Lausanne.getNoOFS());
		erreurs.clear();
		warnings.clear();
		depart.validate(erreurs, warnings);
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

		Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true, null);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		Assert.assertTrue("Absence d'événement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));

	}
	/**
	 * Permet de tester le handle sur une personne seule avec une nouvelle adresse inconnue
	 *
	 */
	@Test
	public void testHandleNouvelleAdresseInconnue() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		Depart depart = createValidDepart(NO_IND_PAUL, DATE_EVENEMENT, true, null);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));
	}

	@Test
	public void testHandleDepartHCFinAnnee() throws Exception {
		final Depart depart = createValidDepart(NO_IND_ALBERT, DATE_EVENEMENT_FIN_ANNEE, true, null);
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

		Depart depart = createValidDepart(1241, DATE_EVENEMENT, false, null);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalFerme.getMotifFermeture());

		Assert.assertNotNull(forFiscalPrincipal.getMotifOuverture());
		Assert.assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		Assert.assertEquals(Integer.valueOf(MockCommune.Zurich.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		Assert.assertTrue("Absence d'evenement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));

	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale elle-même située dans le canton
	 */
	@Test
	public void testHandleDepartSecondaireVaudois() throws Exception {

		Depart depart = createValidDepart(NO_IND_RAMONA, DATE_EVENEMENT, false, null);

		final ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		final PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalFerme.getMotifFermeture());

		Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipal.getMotifOuverture());
		Assert.assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		Assert.assertEquals(Integer.valueOf(MockCommune.Vevey.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		Assert.assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		Assert.assertFalse("Absence d'evenement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));
	}

	/**
	 * Permet de chercher la presence d'un message d'erreur dans la liste des erreurs de type evenement civil
	 *
	 * @param erreurs
	 * @param message
	 * @return
	 */
	private boolean findMessage(List<EvenementCivilExterneErreur> erreurs, String message) {
		boolean isPresent = false;
		for (EvenementCivilExterneErreur evenementErreur : erreurs) {
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
		f = tiersService.addAndSave(tiers, f);
		return f;
	}

	/**
	 * En cas de départ dans un autre canton ou à l’étranger, si la date de l’événement survient après le 25 du mois et que le mode
	 * d’imposition est l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du mois
	 *
	 * Update 22.11.2010 [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
	 */
	@Test
	public void testDateDeFermetureFinDeMois() throws Exception {

		Depart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, true, null);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		// [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
		// RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		RegDate dateAttendu = depart.getDate();
		assertEquals("La date de fermeture est incorrecte", dateAttendu, forFiscalPrincipalFerme.getDateFin());
	}

	/**
	 * En cas de départ dans le canton de Neuchâtel avec l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du
	 * mois précédent l’événement si la date de l’événement est située entre le 1er et le 15 du mois, ces deux dates comprises.
	 *
	 * Update 22.11.2010 [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
	 */
	@Test
	public void testDateDeFermetureNeuchatelDebutMois() throws Exception {

		Depart depart = createValidDepart(1240, DATE_EVENEMENT_DEBUT_MOIS, MockCommune.Neuchatel);
		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		assertNotNull(forFiscalPrincipal);

		// [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
		// RegDate dateAttendu = RegDate.get(2008, 6, 30);
		RegDate dateAttendu = DATE_EVENEMENT_DEBUT_MOIS;

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + " " + dateAttendu, forFiscalPrincipalFerme.getDateFin().equals(dateAttendu));
	}

	/**
	 * En cas de départ dans le canton de Neuchâtel avec l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du
	 * mois de l’événement si la date de l’événement est située après le 15 du mois.
	 * 
	 * Update 22.11.2010 [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
	 */
	@Test
	public void testDateDeFermetureNeuchatelFinMois() throws Exception {

		Depart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, MockCommune.Neuchatel);

		// [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
		// RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		RegDate dateAttendu = depart.getDate();

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertEquals("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + "" + dateAttendu, dateAttendu, forFiscalPrincipalFerme.getDateFin());
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
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 1, 1), dateDepart.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateDepart, null);
			}
		});

		// mise en place des fors
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null);
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
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_AucunFor() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
			}
		});

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addHabitant(noIndividu);
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(0, ff.size());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_ForAnnule() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
			}
		});

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2006, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Bex);
				ffp.setAnnule(true);
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		Assert.assertNotNull(ffp);
		Assert.assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		Assert.assertTrue(ffp.isAnnule());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_ForFerme() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
			}
		});

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2006, 1, 5), MotifFor.ARRIVEE_HS, date(2007, 12, 31), MotifFor.DEPART_HS, MockCommune.Bex);
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		try {
			handleDepart(depart, erreurs, warnings);
			Assert.fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		Assert.assertNotNull(ffp);
		Assert.assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		Assert.assertFalse(ffp.isAnnule());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(date(2007, 12, 31), ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_ForOuvert() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
			}
		});

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2006, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		try {
			handleDepart(depart, erreurs, warnings);
			Assert.fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		Assert.assertNotNull(ffp);
		Assert.assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		Assert.assertFalse(ffp.isAnnule());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertNull(ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_MenageAvecFor() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
				marieIndividu(ind, date(1971, 5, 1));
			}
		});

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(1974, 5, 1), null);
				final MenageCommun mc = ensemble.getMenage();
				addForPrincipal(mc, date(2006, 1, 5), MotifFor.ARRIVEE_HS, MockCommune.Bex);
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		try {
			handleDepart(depart, erreurs, warnings);
			Assert.fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(0, ff.size());

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(pp, null);
		final MenageCommun mc = ensemble.getMenage();
		Assert.assertNotNull(mc);

		final Set<ForFiscal> ffmc = mc.getForsFiscaux();
		Assert.assertNotNull(ffmc);
		Assert.assertEquals(1, ffmc.size());

		final ForFiscal ffp = ffmc.iterator().next();
		Assert.assertNotNull(ffp);
		Assert.assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		Assert.assertFalse(ffp.isAnnule());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertNull(ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	public void testDepartCommuneSansAncienneCommuneConnue_MenageSansFor() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Diagon Alley", "12b", 99999, null, "London", MockPays.RoyaumeUni, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
				marieIndividu(ind, date(1971, 5, 1));
			}
		});

		final RegDate dateMariage = date(1981, 5, 1);

		// mise en place des habitants et de leurs fors (ici : aucun for)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);

				// on crée un for sur le tiers pour pimenter la chose, mais pas sur le couple
				addForPrincipal(pp, date(1974, 4, 30), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin);
				addEnsembleTiersCouple(pp, null, dateMariage, null);
				
				return null;
			}
		});

		// envoi de l'événement de départ
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		Assert.assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		Assert.assertNotNull(ff);
		Assert.assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		Assert.assertNotNull(ffp);
		Assert.assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		Assert.assertFalse(ffp.isAnnule());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Leysin.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin());

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(pp, null);
		final MenageCommun mc = ensemble.getMenage();
		Assert.assertNotNull(mc);
		final Set<ForFiscal> ffmc = mc.getForsFiscaux();
		Assert.assertNotNull(ffmc);
		Assert.assertEquals(0, ffmc.size());
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
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		// mise en place des fors
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				return null;
			}
		});

		// résumons-nous :
		// 1. nous allons maintenant recevoir un événement de départ secondaire depuis Echallens
		// 2. il faudrait que le for principal soit fermé et passe à Genève
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null);
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
	 * [UNIREG-2212] Vérifie qu'un départ d'une résidence secondaire vaudoise au 19 décembre ouvre bien un nouveau for fiscal au 19 décembre sur la nouvelle commune (règle de fin d'année non-active)
	 */
	@Test
	public void testDepartResidenceSecondaire19Decembre() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2009, 12, 19);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(1956, 4, 30), null);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
			}
		});

		// Note (msi) : je ne sais pas s'il est possible d'avoir une configuration de fors fiscaux comme celle ci-dessous. D'après le code, oui,
		// mais d'un point de vue métier, je ne sais pas. Dans tous les cas, j'utilise cette configuration dans le seul but pour tester la règle
		// de décalage des dates de fin d'année.
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(1976, 4, 30), MotifFor.MAJORITE, date(1999, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				return null;
			}
		});

		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(3, ff.size());
		assertForPrincipal(date(1976, 4, 30), MotifFor.MAJORITE, date(1999, 12, 31), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, dateDepart, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(1));
		assertForPrincipal(dateDepart.getOneDayAfter(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ff.get(2));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un départ d'une résidence secondaire vaudoise au 20 décembre ne ferme effectivement le for que le 31 décembre (règle de fin d'année activée)
	 */
	@Test
	public void testDepartResidenceSecondaire20Decembre() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2009, 12, 21);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(1956, 4, 30), null);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
			}
		});

		// Note (msi) : je ne sais pas s'il est possible d'avoir une configuration de fors fiscaux comme celle ci-dessous. D'après le code, oui,
		// mais d'un point de vue métier, je ne sais pas. Dans tous les cas, j'utilise cette configuration dans le seul but pour tester la règle
		// de décalage des dates de fin d'année.
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(1976, 4, 30), MotifFor.MAJORITE, date(1999, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				return null;
			}
		});

		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(3, ff.size());
		assertForPrincipal(date(1976, 4, 30), MotifFor.MAJORITE, date(1999, 12, 31), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2009, 12, 31), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(1));
		assertForPrincipal(date(2010, 1, 1), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ff.get(2));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un départ hors-Canton au 20 décembre ouvre bien un nouveau for fiscal au 21 décembre sur la nouvelle commune (pas de règle de fin d'année)
	 */
	@Test
	public void testDepartHorsCanton20Decembre() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2009, 12, 20);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(1956, 4, 30), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.VoltaStrasse, null, dateDepart.getOneDayAfter(), null);
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(1976, 4, 30), MotifFor.MAJORITE, MockCommune.Lausanne);
				return null;
			}
		});

		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(2, ff.size());
		assertForPrincipal(date(1976, 4, 30), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFSEtendu(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFSEtendu(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ff.get(1));
	}

	/**
	 * vérifie et traite un depart
	 *
	 * @param depart
	 * @return le fort fiscal après le traitement du départ
	 */
	private ForFiscalPrincipal handleDepart(Depart depart) throws EvenementCivilException {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

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
	private void handleDepartSimple(Depart depart) throws EvenementCivilException {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		handleDepart(depart, erreurs, warnings);

		if (!erreurs.isEmpty()) {
			for (EvenementCivilExterneErreur erreur : erreurs) {
				LOGGER.error("Erreur trouvée : " + erreur.getMessage());
			}
			Assert.fail("Une erreur est survenue lors du traitement du départ");
		}
	}

	private void handleDepart(Depart depart, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		depart.checkCompleteness(erreurs, warnings);
		depart.validate(erreurs, warnings);
		depart.handle(warnings);
	}

	public static boolean findEvenementFermetureFor(Collection<EvenementFiscal> lesEvenements, Depart depart) {

		boolean isPresent = false;
		Iterator<EvenementFiscal> iteEvFiscal = lesEvenements.iterator();
		EvenementFiscal evenement = null;
		while (iteEvFiscal.hasNext()) {
			evenement = iteEvFiscal.next();
			if (evenement.getType() == TypeEvenementFiscal.FERMETURE_FOR && evenement.getDateEvenement()==depart.getDate()) {
				isPresent = true;
				break;
			}
		}
		return isPresent;

	}
}
