package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class DepartTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DepartTest.class);

	private static final int NUMERO_INDIVIDU_SEUL = 1234;
	private static final int NO_IND_RAMONA = 1242;
	private static final int NO_IND_PAUL = 1243;
	private static final int NO_IND_ALBERT = 1244;
	private static final int NO_IND_ADRIEN = 1245;

	private static final RegDate DATE_EVENEMENT = RegDate.get(2008, 8, 19);
	private static final RegDate DATE_EVENEMENT_FIN_MOIS = RegDate.get(2008, 7, 26);
	private static final RegDate DATE_EVENEMENT_DEBUT_MOIS = RegDate.get(2008, 7, 10);
	private static final RegDate DATE_EVENEMENT_FIN_ANNEE = RegDate.get(2008, 12, 27);

	private EvenementFiscalService evenementFiscalService;

	//JIRA 1996
	private static final int noIndCharles = 782551;
	private static final int noIndGeorgette = 782552;

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

	//private evenementCivilRegPPDAO evenementCivilDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		loadDatabase(DB_UNIT_DATA_FILE);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu marianne = addIndividu(NUMERO_INDIVIDU_SEUL, RegDate.get(1961, 3, 12), "Durant", "Marianne", false);
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
				adresse.setLieu(MockCommune.Cossonay.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Cossonay.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Zurich.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Cossonay.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Cossonay.getNomOfficiel());
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
				adresse.setLieu(cossonay.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Lausanne.getNomOfficiel());
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
					adresse.setLieu(MockCommune.Zurich.getNomOfficiel());
					adresse.setNumeroPostal("8001");
					adresse.setNumeroOrdrePostal(MockLocalite.Zurich.getNoOrdre());
					adresse.setNpa("8001");
					adresse.setCommuneAdresse(MockCommune.Zurich);
				}
				else {
					adresse.setLocalite(MockLocalite.Vevey.getNomCompletMajuscule());
					adresse.setLieu(MockCommune.Vevey.getNomOfficiel());
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
				adresse.setLieu(MockCommune.Cossonay.getNomOfficiel());
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
				// adresse.setLieu(MockCommune.Zurich.getNomOfficiel());
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
				addOrigine(indCharles, MockCommune.Neuchatel);
				addNationalite(indCharles, MockPays.Suisse, dateNaissanceCharles, null);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, dateDepart);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Enney.CheminDAfflon, null, dateArrivee, null);

				final RegDate dateNaissanceGeorgette = RegDate.get(1946, 5, 14);
				indGorgette = addIndividu(noIndGeorgette, dateNaissanceGeorgette, "CHABOUDEZ", "Georgette", false);
				addOrigine(indGorgette, MockCommune.Neuchatel);
				addNationalite(indGorgette, MockPays.Suisse, dateNaissanceGeorgette, null);
				addAdresse(indGorgette, TypeAdresseCivil.PRINCIPALE,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indGorgette, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indGorgette, TypeAdresseCivil.PRINCIPALE, MockRue.Enney.CheminDAfflon, null, dateArrivee, null);
				marieIndividus(indCharles, indGorgette, dateMariage);
			}

			private void setUpJira2161() {

				// le but sera de lancer un événement de départ secondaire en 2007, alors qu'aucune adresse secondaire n'est active
				final MockIndividu adrien = addIndividu(NO_IND_ADRIEN, RegDate.get(1956, 6, 1), "Nadire", "Adrien", true);
				addAdresse(adrien, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, RegDate.get(2004, 1, 1), null);
				addAdresse(adrien, TypeAdresseCivil.SECONDAIRE, MockRue.Bex.CheminDeLaForet, null, RegDate.get(2006, 1, 20), RegDate.get(2006, 7, 23));
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
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleJira1996() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		Depart departCharles = createValidDepart(noIndCharles, dateDepart, true, null, true);
		Depart departGeorgette = createValidDepart(noIndGeorgette, dateDepart, true, null, true);

		handleDepartSimple(departCharles);
		handleDepartSimple(departGeorgette);
		Tiers tiers = tiersDAO.get(26012004L);
		ForFiscalPrincipal forFiscalPrincipalOuvert = tiers.getForFiscalPrincipalAt(dateArrivee);

		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalOuvert.getTypeAutoriteFiscale());
		assertTrue(communeArrivee.getNoOFS() == forFiscalPrincipalOuvert.getNumeroOfsAutoriteFiscale());



		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(tiers);
		assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		assertTrue("Absence d'événement de type femeture de for", findEvenementFermetureFor(lesEvenements, departCharles));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleJira2161() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ secondaire alors qu'aucune adresse secondaire n'est valide");

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(NO_IND_ADRIEN);
				return null;
			}
		});
		
		final Depart depart = createValidDepart(NO_IND_ADRIEN, RegDate.get(2007, 6, 30), false, null, true);

		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);

		assertEquals(1, collector.getErreurs().size());

		final EvenementCivilErreur erreur = collector.getErreurs().get(0);
		assertNotNull(erreur);
		assertEquals("Adresse de résidence avant départ inconnue", erreur.getMessage());
	}

	/**
	 * Teste la complétude du départ d'un individu seul.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckCompletenessIndividuSeul() throws Exception {

		final MessageCollector collector = buildMessageCollector();
		LOGGER.debug("Test départ individu seul...");
		final DepartPrincipal depart = (DepartPrincipal) createValidDepart(1234, DATE_EVENEMENT, true, null, true);
		depart.checkCompleteness(collector, collector);
		assertTrue("individu célibataire : ca n'aurait pas du causer une erreur", collector.getErreurs().isEmpty());
		LOGGER.debug("Test départ individu seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckCompletenessIndividuMarieSeul() throws Exception {

		final MessageCollector collector = buildMessageCollector();

		LOGGER.debug("Test départ individu marié seul...");

		final DepartPrincipal depart = (DepartPrincipal) createValidDepart(1235, DATE_EVENEMENT, true, null, true);
		depart.checkCompleteness(collector, collector);
		assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", collector.getErreurs().isEmpty());
		LOGGER.debug("Test départ individu marié seul : OK");

	}

	/**
	 * Teste la complétude du départ d'un individu Marié seul.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckCompletenessIndividuMarie() throws Exception {

		final MessageCollector collector = buildMessageCollector();

		LOGGER.debug("Test départ individu marié ...");

		DepartPrincipal depart = (DepartPrincipal) createValidDepart(1236, DATE_EVENEMENT, true, null, true);
		depart.checkCompleteness(collector, collector);
		assertTrue("individu célibataire marié seul : ca n'aurait pas du causer une erreur", collector.getErreurs().isEmpty());
		LOGGER.debug("Test départ individu marié  : OK");
	}

	private Depart createValidDepart(long noIndividu, RegDate dateEvenement, boolean principale, @Nullable Integer overrideNoOfsCommuneAnnonce, boolean isRegPP) throws Exception {

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);

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
			noOFS = MockCommune.Croy.getNoOFS();
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

		if (principale) {
			return new DepartPrincipal(individu, null, dateEvenement, noOFS, adressePrincipale, communeVd, nouvelleAdresse, communeHorsVd, context, isRegPP);
		}
		else {
			return new DepartSecondaire(individu, null, dateEvenement, noOFS, nouvelleAdresse, communeHorsVd, adresseSecondaire, communeSecondaire, context, isRegPP);
		}
	}

	private Depart createValidDepart(long noIndividu, RegDate dateEvenement, MockCommune nouvelleCommune, boolean isRegPP) throws Exception {

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);


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

		return new DepartPrincipal(individu, null, dateEvenement, noOFS, adressePrincipale, communeVd, nouvelleAdresse, nouvelleCommune, context, isRegPP);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDepartAnterieurPrincipale() throws Exception {
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle...");

		// mise-en-place des données à DATE_EVENEMENT
		final Individu individu = serviceCivil.getIndividu((long) 1234, null);
		final AdressesCiviles adresseVaud = new AdressesCiviles(serviceCivil.getAdresses((long) 1234, DATE_EVENEMENT, false));
		final MockAdresse adressePrincipale = (MockAdresse) adresseVaud.principale;
		adressePrincipale.setDateFinValidite(DATE_EVENEMENT);

		final MockCommune communeVd = (MockCommune) serviceInfra.getCommuneByAdresse(adressePrincipale, DATE_EVENEMENT);
		int noOFS = communeVd.getNoOFS();
		final AdressesCiviles adresseHorsVaud = new AdressesCiviles(serviceCivil.getAdresses((long) 1234, DATE_EVENEMENT.getOneDayAfter(), false));
		final MockAdresse nouvelleAdresse = (MockAdresse) adresseHorsVaud.principale;
		final MockCommune communeHorsVd = (MockCommune) serviceInfra.getCommuneByAdresse(nouvelleAdresse, DATE_EVENEMENT.getOneDayAfter());

		// création d'un événement au 19 novembre 1970
		final Depart depart = new DepartPrincipal(individu, null, date(1970, 11, 19), noOFS, adressePrincipale, communeVd, nouvelleAdresse, communeHorsVd, context, true);

		final MessageCollector collector = buildMessageCollector();
		depart.validate(collector, collector);

		assertTrue("Le départ est antérieur à la date de fin de validité de l'adresse actuelle, une erreur aurait du être déclenchée", findMessage(collector.getErreurs(), "La date de départ est différente"));
		LOGGER.debug("Test départ antérieur à la date de fin de validité de l'adresse actuelle : OK");
	}

	/**
	 * Teste de validation que la nouvelle commune principale n'est pas dans le canton de vaud
	 * Selon SIFISC-4230 et SIFISC-4584 les départs vaudois sont ignorés
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateNouvelleCommunePrinHorsCanton() throws Exception {

		LOGGER.debug("Test si la nouvelle commune principale est hors canton...");

		final MessageCollector collector = buildMessageCollector();
		try {
			Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, MockCommune.Cossonay, true);
			fail("On attendait une exception parce que la  nouvelle commune est dans le canton de Vaud");
		}
		catch (EvenementCivilException e) {
			assertEquals("La nouvelle commune est toujours dans le canton de Vaud", e.getMessage());
		}

		LOGGER.debug("Test nouvelle commune hors canton : OK");

	}

	/**
	 * Teste de validation de la commune d'annonce
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCommuneAnnoncePrincipal() throws Exception {

		LOGGER.debug("Teste si la commune d'annonce principale est correcte...");

		final MessageCollector collector = buildMessageCollector();
		Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true, MockCommune.Lausanne.getNoOFS(), true);
		depart.validate(collector, collector);
		assertTrue("La commune d'anonce et differente de celle de la dernière adresse, une erreur aurait du être déclenchée", findMessage(collector.getErreurs(), "La commune d'annonce"));
		LOGGER.debug("Test commune d'annonce principale : OK");

	}

	/**
	 * Permet de tester le handle sur une personne seule
	 *
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		Depart depart = createValidDepart(NUMERO_INDIVIDU_SEUL, DATE_EVENEMENT, true, null, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		assertTrue("Absence d'événement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));

	}
	/**
	 * Permet de tester le handle sur une personne seule avec une nouvelle adresse inconnue
	 *
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNouvelleAdresseInconnue() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		Depart depart = createValidDepart(NO_IND_PAUL, DATE_EVENEMENT, true, null, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());

		assertTrue("Pas de nouveau for fiscal ouvert", forFiscalPrincipal.getDateDebut() == depart.getDate().getOneDayAfter());
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		assertTrue("Absence d'evenement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleDepartHCFinAnnee() throws Exception {
		final Depart depart = createValidDepart(NO_IND_ALBERT, DATE_EVENEMENT_FIN_ANNEE, true, null, true);
		final ForFiscalPrincipal ffp = handleDepart(depart);
		assertEquals("Le for HC aurait dû être ouvert encore l'année du départ", DATE_EVENEMENT_FIN_ANNEE.getOneDayAfter(), ffp.getDateDebut());
	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale située hors canton
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleDepartSecondaireHorsCanton() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de départ d'une residence secondaire vaudoise.");

		Depart depart = createValidDepart(1241, DATE_EVENEMENT, false, null, true);

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(depart.getDate());
		assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalFerme.getMotifFermeture());

		assertNotNull(forFiscalPrincipal.getMotifOuverture());
		assertEquals(depart.getDate().getOneDayAfter(), forFiscalPrincipal.getDateDebut());
		assertEquals(Integer.valueOf(MockCommune.Zurich.getNoOFS()), forFiscalPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(forFiscalPrincipalFerme.getModeImposition(), forFiscalPrincipal.getModeImposition());
		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

		Collection<EvenementFiscal> lesEvenements = evenementFiscalService.getEvenementsFiscaux(forFiscalPrincipalFerme.getTiers());
		assertNotNull("Pas d'événement fiscal engendré", lesEvenements);
		assertTrue("Absence d'evenement de type femeture de for", findEvenementFermetureFor(lesEvenements, depart));

	}

	/**
	 * Teste le cas où un contribuable bénéficiant d'un arrangement fiscal (= for principal ouvert sur une résidence secondaire dans le
	 * canton) quitte sa résidence secondaire pour sa résidence principale elle-même située dans le canton
	 *
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleDepartSecondaireVaudois() throws Exception {

		Depart depart = createValidDepart(NO_IND_RAMONA, DATE_EVENEMENT, false, null, false);

		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);

		final PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		String message = String.format(
				"A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois actif (arrangement fiscal ?)",
				tiers.getNumero());
		assertTrue("L'évènement devrait partir en erreur car c'est un départ vaudois sur une résidence secondaire", collector.hasErreurs());
		assertEquals(message, collector.getErreurs().get(0).getMessage());

		LOGGER.debug("Test de traitement d'un événement de départ residence secondaire vaudois OK");

	}

	/**
	 * Permet de chercher la presence d'un message d'erreur dans la liste des erreurs de type evenement civil
	 *
	 * @param erreurs
	 * @param message
	 * @return
	 */
	private boolean findMessage(List<? extends EvenementCivilErreur> erreurs, String message) {
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
		f = tiersDAO.addAndSave(tiers, f);
		return f;
	}

	/**
	 * En cas de départ dans un autre canton ou à l’étranger, si la date de l’événement survient après le 25 du mois et que le mode
	 * d’imposition est l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du mois
	 *
	 * Update 22.11.2010 [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDeFermetureFinDeMois() throws Exception {

		Depart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, true, null, true);

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
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDeFermetureNeuchatelDebutMois() throws Exception {

		Depart depart = createValidDepart(1240, DATE_EVENEMENT_DEBUT_MOIS, MockCommune.Neuchatel, true);
		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		assertNotNull(forFiscalPrincipal);

		// [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
		// RegDate dateAttendu = RegDate.get(2008, 6, 30);
		RegDate dateAttendu = DATE_EVENEMENT_DEBUT_MOIS;

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertTrue("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + ' ' + dateAttendu, forFiscalPrincipalFerme.getDateFin().equals(dateAttendu));
	}

	/**
	 * En cas de départ dans le canton de Neuchâtel avec l’une des formes d’impôt à la source, le for principal est fermé au dernier jour du
	 * mois de l’événement si la date de l’événement est située après le 15 du mois.
	 * 
	 * Update 22.11.2010 [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDeFermetureNeuchatelFinMois() throws Exception {

		Depart depart = createValidDepart(1239, DATE_EVENEMENT_FIN_MOIS, MockCommune.Neuchatel, true);

		// [UNIREG-2212] : les dates ne sont plus ajustées dans ce cas-là.
		// RegDate dateAttendu = depart.getDate().getLastDayOfTheMonth();
		RegDate dateAttendu = depart.getDate();

		ForFiscalPrincipal forFiscalPrincipal = handleDepart(depart);
		assertNotNull(forFiscalPrincipal);

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		ForFiscalPrincipal forFiscalPrincipalFerme = tiers.getForFiscalPrincipalAt(dateAttendu);

		assertEquals("La date de fermeture est incorrect:" + forFiscalPrincipalFerme.getDateFin() + dateAttendu, dateAttendu, forFiscalPrincipalFerme.getDateFin());
	}

	/**
	 * [UNIREG-1921] En cas de départ secondaire de la commune où se situe également le for principal, rien à faire sur les fors
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		// 3. On devrait avoir une erreur car on est dans le cas d'un départ vaudois avec for Principal
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, false);
		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		String message = String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois actif (arrangement fiscal ?)",
				pp.getNumero());
		assertTrue("L'évènement devrait partir en erreur car c'est un départ vaudois sur une résidence secondaire", collector.hasErreurs());
		assertEquals(message,collector.getErreurs().get(0).getMessage());



		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(2, ff.size());

		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		assertNotNull(ffp);
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertEquals(dateDepart, ffp.getDateDebut());
	}

	/**
	 * [SIFISC-6841] Vérifie que le départ d'une résidence secondaire pour une destination inconnue transforme bien la personne physique en non-habitante.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDepartSecondaireDeCommuneDestinationInconnuePersonnePhysiqueSansFor() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, ServiceInfrastructureRaw.noPaysInconnu, null));
			}
		});

		// mise en place de la personne physique
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				addHabitant(noIndividu);
				return null;
			}
		});

		// résumons-nous :
		// 1. la personne physique est habitante à Echallens en résidence secondaire
		// 2. nous allons maintenant recevoir un événement de départ secondaire depuis Echallens pour une destination inconnue
		// 3. on devrait avoir la fermeture du for fiscal et le passage en non-habitante de la personne physique
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, false);
		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);
		assertEmpty(collector.getErreurs());

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);
		assertFalse(pp.isHabitantVD());
		assertEmpty(pp.getForsFiscauxSorted());
	}

	/**
	 * [SIFISC-6842] Vérifie que le départ d'une résidence principale alors qu'il y a encore une résidence secondaire dans le canton laisse bien le flag 'habitant' actif.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDepartPrincipalAlorsQueEncoreEnResidenceSecondaire() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, date(2000, 1, 1), dateDepart);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
			}
		});

		// mise en place de la personne physique
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000,1,1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return null;
			}
		});

		// résumons-nous :
		// 1. la personne physique est habitante à Lausanne en résidence principal et à Echallens en résidence secondaire
		// 2. nous allons maintenant recevoir un événement de départ secondaire depuis Lausanne
		// 3. on devrait avoir la fermeture du for fiscal de Lausanne mais la personne physique devrait rester habitante
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, false);
		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);
		assertEmpty(collector.getErreurs());

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);
		assertTrue(pp.isHabitantVD());

		final List<ForFiscal> fors = pp.getForsFiscauxSorted();
		assertNotNull(fors);
		assertEquals(2, fors.size());
		assertForPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
		                   (ForFiscalPrincipal) fors.get(0));
		assertForPrincipal(dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, TypeAutoriteFiscale.PAYS_HS, MockPays.France.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
		                   (ForFiscalPrincipal) fors.get(1));
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(0, ff.size());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		assertNotNull(ffp);
		assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		assertTrue(ffp.isAnnule());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		final MessageCollector collector = buildMessageCollector();
		try {
			handleDepart(depart, collector, collector);
			fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		assertNotNull(ffp);
		assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		assertFalse(ffp.isAnnule());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertEquals(date(2007, 12, 31), ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		final MessageCollector collector = buildMessageCollector();
		try {
			handleDepart(depart, collector, collector);
			fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		assertNotNull(ffp);
		assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		assertFalse(ffp.isAnnule());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertNull(ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		final MessageCollector collector = buildMessageCollector();
		try {
			handleDepart(depart, collector, collector);
			fail("On attendait une exception parce que la commune de départ n'a pas pu être trouvée");
		}
		catch (EvenementCivilException e) {
			assertEquals("La commune de départ n'a pas été trouvée", e.getMessage());
		}

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(0, ff.size());

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(pp, null);
		final MenageCommun mc = ensemble.getMenage();
		assertNotNull(mc);

		final Set<ForFiscal> ffmc = mc.getForsFiscaux();
		assertNotNull(ffmc);
		assertEquals(1, ffmc.size());

		final ForFiscal ffp = ffmc.iterator().next();
		assertNotNull(ffp);
		assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		assertFalse(ffp.isAnnule());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertNull(ffp.getDateFin());
	}

	/**
	 * [UNIREG-2701] En cas de départ principal sur un individu dont l'ancienne commune est inconnue, on traite quand-même l'événement si
	 * le contribuable (ou son ménage actif à la date de l'événement de départ) n'a aucun for non-annulé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(1, ff.size());

		final ForFiscal ffp = ff.iterator().next();
		assertNotNull(ffp);
		assertTrue("Class " + ffp.getClass(), ffp instanceof ForFiscalPrincipal);
		assertFalse(ffp.isAnnule());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Leysin.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin());

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(pp, null);
		final MenageCommun mc = ensemble.getMenage();
		assertNotNull(mc);
		final Set<ForFiscal> ffmc = mc.getForsFiscaux();
		assertNotNull(ffmc);
		assertEquals(0, ffmc.size());
	}

	/**
	 * [UNIREG-1921] En cas de départ secondaire d'une commune vaudoise alors que l'adresse de domicile est hors-canton,
	 * le for vaudois doit se fermer...
	 * [SIFISC-5970] ...et la personne physique doit passer non-habitante
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(2, ff.size());

		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		assertNotNull(ffp);
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
		assertEquals(MockCommune.Geneve.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
		assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());

		// [SIFISC-5970] la personne physique doit maintenant être non-habitante
		assertFalse(pp.isHabitantVD());
	}

	/**
	 * [UNIREG-1921] En cas de départ secondaire d'une commune vaudoise alors que l'adresse de domicile est hors-Suisse,
	 * le for vaudois doit se fermer...
	 * [SIFISC-5970] ...et la personne physique doit passer non-habitante
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDepartSecondaireAvecResidencePrincipaleHS() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2008, 12, 4);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, null, "Vlamverdam", "Gröeg", MockPays.Danemark, date(2000, 1, 1), null);
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
		// 2. il faudrait que le for principal soit fermé et passe au Danemark
		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final Set<ForFiscal> ff = pp.getForsFiscaux();
		assertNotNull(ff);
		assertEquals(2, ff.size());

		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		assertNotNull(ffp);
		assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
		assertEquals(MockPays.Danemark.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
		assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
		assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());

		// [SIFISC-5970] la personne physique doit maintenant être non-habitante
		assertFalse(pp.isHabitantVD());
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un départ d'une résidence secondaire vaudoise au 19 décembre ouvre bien un nouveau for fiscal au 19 décembre sur la nouvelle commune (règle de fin d'année non-active)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDepartResidenceSecondaire19Decembre() throws Exception {

		final long noIndividu = 123456L;
		final RegDate dateDepart = date(2009, 12, 19);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1956, 4, 30), "Talon", "Achille", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, date(1956, 4, 30), null);
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
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens,MotifRattachement.DOMICILE);
				return null;
			}
		});

		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, true);

		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(2, ff.size());
		assertForPrincipal(date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, dateDepart, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS(),
		                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), MotifRattachement.DOMICILE,
		                   ModeImposition.ORDINAIRE, ff.get(1));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un départ d'une résidence secondaire vaudoise au 20 décembre ne ferme effectivement le for que le 31 décembre (règle de fin d'année activée)
	 * [SIFISC-4230] Suite à la décision de mettre en erreur les départ VD depuis une commune secondaire, ce test n'est plus applicable, car il ne concernait que
	 * le cas du déménagement vaudois.
	 */
	@Ignore
	@Transactional(rollbackFor = Throwable.class)
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

		final Depart depart = createValidDepart(noIndividu, dateDepart, false, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(3, ff.size());
		assertForPrincipal(date(1976, 4, 30), MotifFor.MAJORITE, date(1999, 12, 31), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2009, 12, 31), MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(1));
		assertForPrincipal(date(2010, 1, 1), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ff.get(2));
	}

	/**
	 * [UNIREG-2212] Vérifie qu'un départ hors-Canton au 20 décembre ouvre bien un nouveau for fiscal au 21 décembre sur la nouvelle commune (pas de règle de fin d'année)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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

		final Depart depart = createValidDepart(noIndividu, dateDepart, true, null, true);
		handleDepartSimple(depart);

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		assertNotNull(pp);

		final List<ForFiscalPrincipal> ff = pp.getForsFiscauxPrincipauxActifsSorted();
		assertNotNull(ff);
		assertEquals(2, ff.size());
		assertForPrincipal(date(1976, 4, 30), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ff.get(0));
		assertForPrincipal(dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ff.get(1));
	}

	@Test
	public void testDepartAujourdhui() throws Exception {

		final long noIndividu = 167452347546L;
		final RegDate dateOuvertureFor = date(1999, 9, 12);
		final RegDate today = RegDate.get();

		// préparation civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1970, 4, 12), "Petipoint", "Justin", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminTraverse, null, dateOuvertureFor, today);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, today.getOneDayAfter(), null);
			}
		});

		// préparation fiscale
		final long ppid = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateOuvertureFor, MotifFor.INDETERMINE, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement de départ (aujourd'hui)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Depart depart = createValidDepart(noIndividu, today, true, MockCommune.Aubonne.getNoOFS(), true);
				final MessageCollector collector = buildMessageCollector();
				handleDepart(depart, collector, collector);
				assertEquals(1, collector.getErreurs().size());

				final EvenementCivilErreur erreur = collector.getErreurs().get(0);
				assertEquals("Un départ HC/HS ne peut être traité qu'à partir du lendemain de sa date d'effet", erreur.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateOuvertureFor, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals(MockCommune.Aubonne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}

	@Test
	public void testDepartHier() throws Exception {

		final long noIndividu = 167452347546L;
		final RegDate dateDepart = RegDate.get().getOneDayBefore();

		// préparation civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1970, 4, 12), "Petipoint", "Justin", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminTraverse, null, date(1999, 9, 12), dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
			}
		});

		// préparation fiscale
		final long ppid = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(1999, 9, 12), MotifFor.INDETERMINE, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement de départ (hier)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Depart depart = createValidDepart(noIndividu, dateDepart, true, MockCommune.Aubonne.getNoOFS(), true);
				final MessageCollector collector = buildMessageCollector();
				handleDepart(depart, collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				assertEquals(MockCommune.Geneve.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});
	}

	/**
	 * vérifie et traite un depart
	 *
	 * @param depart
	 * @return le fort fiscal après le traitement du départ
	 */
	private ForFiscalPrincipal handleDepart(Depart depart) throws EvenementCivilException {
		LOGGER.debug("Test de traitement d'un événement de départ vaudois.");

		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);

		assertFalse("Une erreur est survenue lors du traitement du départ", collector.hasErreurs());

		PersonnePhysique tiers = tiersDAO.getPPByNumeroIndividu(depart.getNoIndividu());
		assertNotNull(tiers);

		ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(depart.getDate().getOneDayAfter());
		assertNotNull("Le contribuable n'a aucun for fiscal", forFiscalPrincipal);

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

		final MessageCollector collector = buildMessageCollector();
		handleDepart(depart, collector, collector);

		if (collector.hasErreurs()) {
			for (EvenementCivilErreur erreur : collector.getErreurs()) {
				LOGGER.error("Erreur trouvée : " + erreur.getMessage());
			}
			fail("Une erreur est survenue lors du traitement du départ");
		}
	}

	private void handleDepart(Depart depart, EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		depart.validate(erreurs, warnings);
		if (!erreurs.hasErreurs()) {
			depart.handle(warnings);
		}
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
