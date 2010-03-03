package ch.vd.uniregctb.evenement.depart;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

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
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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

	private static final RegDate DATE_EVENEMENT = RegDate.get(2008, 8, 19);
	private static final RegDate DATE_EVENEMENT_FIN_MOIS = RegDate.get(2008, 7, 26);
	private static final RegDate DATE_EVENEMENT_DEBUT_MOIS = RegDate.get(2008, 7, 10);
	private static final RegDate DATE_EVENEMENT_FIN_ANNEE = RegDate.get(2008, 12, 27);

	private static final RegDate DATE_ANTERIEURE_ADRESSE_ACTUELLE = RegDate.get(1940, 11, 19);

	private EvenementFiscalService evenementFiscalService;

	//private EvenementCivilRegroupeDAO evenementCivilDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		//evenementCivilDAO = getBean(EvenementCivilRegroupeDAO.class, "evenementCivilRegroupeDAO");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu marianne = addIndividu(1234, RegDate.get(1961, 3, 12), "Durant", "Marianne", false);
				MockIndividu jhonny = addIndividu(1235, RegDate.get(1961, 3, 12), "Duretique", "Jhonny", true);
				MockIndividu Lucien = addIndividu(1236, RegDate.get(1961, 3, 12), "muller", "Lucien", true);
				MockIndividu Eva = addIndividu(1237, RegDate.get(1961, 3, 12), "muller", "Eva", false);
				MockIndividu george = addIndividu(1238, RegDate.get(1961, 3, 12), "Durant", "George", false);
				MockIndividu antoine = addIndividu(1239, RegDate.get(1961, 3, 12), "Duprés", "Antoine", true);
				MockIndividu angela = addIndividu(1240, RegDate.get(1961, 3, 12), "kob", "Angela", false);
				MockIndividu gege = addIndividu(1241, RegDate.get(1961, 3, 12), "Gégé", "Aglae", false);
				MockIndividu ramona = addIndividu(NO_IND_RAMONA, RegDate.get(1961, 3, 12), "Ramona", "Cheminée", false);
				MockIndividu paul = addIndividu(NO_IND_PAUL, RegDate.get(1961, 3, 12), "Ovent", "Paul", true);
				MockIndividu albert = addIndividu(NO_IND_ALBERT, RegDate.get(1961, 3, 12), "Pittet", "Albert", true);

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
	 * Teste la complétude du départ d'un individu seul.
	 */
	@Test
	public void testCheckCompletenessIndividuSeul() throws Exception {

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		LOGGER.debug("Test départ individu seul...");
		MockDepart depart = createValidDepart(1234, DATE_EVENEMENT, true);
		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire : ca n'aurait pas du causer une erreur");
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
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
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
		Assert.isTrue(erreurs.isEmpty(), "individu célibataire marié seul : ca n'aurait pas du causer une erreur");
		LOGGER.debug("Test départ individu marié  : OK");
	}

	/**
	 * Teste les différents scénarios devant échouer à la validation.
	 */
	private MockDepart createValidDepart(int noIndividu, RegDate dateEvenement, boolean principale) throws Exception {

		MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndividu, 0);

		MockDepart depart = new MockDepart();
		if (principale) {
			depart.setType(TypeEvenementCivil.DEPART_COMMUNE);
		}
		else {
			depart.setType(TypeEvenementCivil.DEPART_SECONDAIRE);
		}

		depart.setIndividu(individu);

		// Adresse actuelle
		AdressesCiviles adresseVaud = serviceCivil.getAdresses(noIndividu, dateEvenement, false);

		MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		// Initialisation d'une date de fin de validité pour la résidence principale
		adressePrincipale.setDateFinValidite(dateEvenement);
		// Adresse principale
		depart.setAncienneAdressePrincipale(adressePrincipale);

		depart.setAncienneAdresseCourrier(adresseVaud.courrier);

		// Commune dans vd
		MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale);

		depart.setAncienneCommunePrincipale(communeVd);

		// Nouvelles adresses
		AdressesCiviles adresseHorsVaud = serviceCivil.getAdresses(noIndividu, dateEvenement.getOneDayAfter(), false);

		MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;
		depart.setAdressePrincipale(nouvelleAdresse);

		// Commune hors vd
		MockCommune communeHorsVd = (MockCommune) serviceInfra.getCommuneByAdresse(nouvelleAdresse);

		depart.setNouvelleCommunePrincipale(communeHorsVd);
		depart.setNumeroOfsCommuneAnnonce(communeVd.getNoOFS());
		depart.setDate(dateEvenement);

		// En cas de depart d'une residence secondaire
		if (!principale) {
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

		Assert.isTrue(findMessage(erreurs, "La date de départ est différente"),
				"Le départ est antérieur à la date de fin de validité de l'adresse actuelle, une erreur aurait du être déclenchée");
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
		Assert.isTrue(findMessage(erreurs, "est toujours dans le canton de Vaud"),
				"La nouvelle commune est dans le canton de Vaud, une erreur aurait du être déclenchée");
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
		Assert.isTrue(findMessage(erreurs, "La commune d'annonce"), "La commune d'anonce et differente de celle "
				+ "de la derniére adresse, une erreur aurait du être déclenchée");
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
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.isTrue(forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter(), "Pas de nouveau for fiscal ouvert");
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.isTrue(MockDepart.findEvenementFermetureFor(lesEvenements, depart), "Absence d'evenement de type femeture de for");

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
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		Assert.isTrue(forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter(), "Pas de nouveau for fiscal ouvert");
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.isTrue(MockDepart.findEvenementFermetureFor(lesEvenements, depart), "Absence d'evenement de type femeture de for");
	}

	@Test
	public void testHandleDepartHCFinAnnee() throws Exception {
		final MockDepart depart = createValidDepart(NO_IND_ALBERT, DATE_EVENEMENT_FIN_ANNEE, true);
		final ForFiscalPrincipal ffp = handleDepart(depart);
		assertEquals("Le for HC aurait dû être ouvert encore l'année du départ", DATE_EVENEMENT_FIN_ANNEE.getOneDayAfter(), ffp.getDateDebut());
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

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalFerme.getMotifFermeture());

		assertNotNull(forFiscalPrincipal.getMotifOuverture());
		assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		assertEquals(Integer.valueOf(MockCommune.Zurich.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.isTrue(MockDepart.findEvenementFermetureFor(lesEvenements, depart), "Absence d'evenement de type femeture de for");

	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale elle-même située dans le canton
	 */
	@Test
	public void testHandleDepartSecondaireVaudois() throws Exception {

		MockDepart depart = createValidDepart(NO_IND_RAMONA, DATE_EVENEMENT, false);

		final ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		final PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalFerme.getMotifFermeture());

		assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipal.getMotifOuverture());
		assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		assertEquals(Integer.valueOf(MockCommune.Vevey.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementFiscals(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'évènement fiscales engendrés", lesEvenements);
		Assert.isTrue(!MockDepart.findEvenementFermetureFor(lesEvenements, depart), "Absence d'evenement de type femeture de for");

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
		assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		assertEquals("La date de fermeture est incorrecte", dateAttendu, forFiscalPrincipalFerme.getDateFin());
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
		assertNotNull(forFiscalPrincipal);
		RegDate dateAttendu = RegDate.get(2008, 6, 30);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + " " + dateAttendu,
				forFiscalPrincipalFerme.getDateFin().equals(dateAttendu));
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
		assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + "" + dateAttendu, forFiscalPrincipalFerme
				.getDateFin().equals(dateAttendu));
		LOGGER.debug("Test de date de fermeture pour un depart vers Neuchatel après le 15 OK");
	}

	/**
	 * vérifie et traite un depart
	 *
	 * @param depart
	 * @return le fort fiscal après le traitement du départ
	 */
	private ForFiscalPrincipal handleDepart(Depart depart) {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(depart, erreurs, warnings);
		evenementCivilHandler.validate(depart, erreurs, warnings);
		evenementCivilHandler.handle(depart, warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement du départ");

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getIndividu().getNoTechnique());
		assertNotNull(tiers);

		ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(depart.getDate().getOneDayAfter());
		assertNotNull("Le contribuable n'a aucun for fiscal", forFiscalPrincipal);

		return forFiscalPrincipal;
	}


}
