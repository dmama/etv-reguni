package ch.vd.uniregctb.evenement.arrivee;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.arrivee.ArriveeHandler.ArriveeType;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ArriveeHandlerExtTest extends AbstractEvenementHandlerTest {

	private EvenementCivilHandler arriveeHandler;

	@Override
	public String getHandlerBeanName() {
		return "arriveeHandler";
	}

	public ArriveeHandlerExtTest() {
		setWantIndexation(true);
	}
	
	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		arriveeHandler = evenementCivilHandler;
	}

	@Test
	public void testCompletenessIndividuSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setIndividu(individu);
		arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
		arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		arriveeHandler.checkCompleteness(arrivee, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		MockArrivee sansIndividu = (MockArrivee) arrivee.clone();
		sansIndividu.setIndividu(null);
		arriveeHandler.checkCompleteness(sansIndividu, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleAdressePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleAdressePrincipal.setNouvelleAdressePrincipale(null);
		arriveeHandler.checkCompleteness(sansNouvelleAdressePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleCommunePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleCommunePrincipal.setNouvelleCommunePrincipale(null);
		sansNouvelleCommunePrincipal.setNumeroOfsCommuneAnnonce(0);
		arriveeHandler.checkCompleteness(sansNouvelleCommunePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	@Test
	public void testCompletenessIndividuMarieSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				// marie l'individu, mais seul
				marieIndividu(pierre, RegDate.get(1985, 07, 11));
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setIndividu(individu);
		arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		arriveeHandler.checkCompleteness(arrivee, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		MockArrivee sansIndividu = (MockArrivee) arrivee.clone();
		sansIndividu.setIndividu(null);
		arriveeHandler.checkCompleteness(sansIndividu, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleAdressePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleAdressePrincipal.setNouvelleAdressePrincipale(null);
		arriveeHandler.checkCompleteness(sansNouvelleAdressePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleCommunePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleCommunePrincipal.setNouvelleCommunePrincipale(null);
		sansNouvelleCommunePrincipal.setNumeroOfsCommuneAnnonce(0);
		arriveeHandler.checkCompleteness(sansNouvelleCommunePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	@Test
	public void testCompletenessIndividuMarie() throws Exception {

		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final long noIndividuHorsCouple = 3;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1977, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null, MockLocalite.LesClees, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null, MockLocalite.LesClees, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null, MockLocalite.LesClees, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null, MockLocalite.LesClees, RegDate.get(1980,
						1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));

				// individu hors-couple
				MockIndividu sophie = addIndividu(noIndividuHorsCouple, RegDate.get(1964, 4, 8), "Dupuis", "Sophie", false);
				addAdresse(sophie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1982, 3, 2), null);
				addAdresse(sophie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1982, 3, 2), null);
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final Individu individuHorsCouple = serviceCivil.getIndividu(noIndividuHorsCouple, dateArrivee.year());
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);

		/*
		 * Construit un événement d'arrivée minimal et valide
		 */
		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setIndividu(individuPrincipal);
		arrivee.setConjoint(individuConjoint);
		arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arrivee.setNouvelleCommunePrincipale(MockCommune.Lausanne);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

		/*
		 * Vérification de l'intégrité de l'événement minimal
		 */
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		arriveeHandler.checkCompleteness(arrivee, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());

		/*
		 * Détection d'erreurs pour les différents cas d'événements invalides.
		 */
		MockArrivee sansIndividu = (MockArrivee) arrivee.clone();
		sansIndividu.setIndividu(null);
		arriveeHandler.checkCompleteness(sansIndividu, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansConjoint = (MockArrivee) arrivee.clone();
		sansConjoint.setConjoint(null);
		arriveeHandler.checkCompleteness(sansConjoint, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleAdressePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleAdressePrincipal.setNouvelleAdressePrincipale(null);
		arriveeHandler.checkCompleteness(sansNouvelleAdressePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee sansNouvelleCommunePrincipal = (MockArrivee) arrivee.clone();
		sansNouvelleCommunePrincipal.setNouvelleCommunePrincipale(null);
		sansNouvelleCommunePrincipal.setNumeroOfsCommuneAnnonce(0);
		arriveeHandler.checkCompleteness(sansNouvelleCommunePrincipal, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		MockArrivee avecConjointHorsCouple = (MockArrivee) arrivee.clone();
		avecConjointHorsCouple.setConjoint(individuHorsCouple);
		arriveeHandler.checkCompleteness(avecConjointHorsCouple, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	@Test
	public void testValidateDateDemenagement() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			tiersDAO.save(habitant);
		}

		/*
		 * Ok : événement d'arrivée à date courante
		 */
		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setIndividu(individu);
		arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
		arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
		arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		arriveeHandler.validate(arrivee, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();

		/*
		 * Ok : événement d'arrivée rétro-actif sans information d'ancienne adresse
		 */
		MockArrivee arriveeRetroActiveDeNullePart = new MockArrivee();
		arriveeRetroActiveDeNullePart.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arriveeRetroActiveDeNullePart.setDate(dateArrivee);
		arriveeRetroActiveDeNullePart.setIndividu(individu);
		arriveeRetroActiveDeNullePart.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arriveeRetroActiveDeNullePart.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arriveeRetroActiveDeNullePart.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		arriveeHandler.checkCompleteness(arriveeRetroActiveDeNullePart, erreurs, warnings);
		arriveeHandler.validate(arriveeRetroActiveDeNullePart, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		arriveeHandler.handle(arriveeRetroActiveDeNullePart, warnings);
		assertTrue(erreurs.isEmpty());
		assertEquals(1, warnings.size());
		erreurs.clear();
		warnings.clear();

		/*
		 * Erreur: évenement d'arrivée rétro-actif situé avant la date de début de validité de l'ancienne adresse
		 */
		MockArrivee arriveeRetroActive = new MockArrivee();
		arriveeRetroActive.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arriveeRetroActive.setDate(RegDate.get(1945, 1, 1));
		arriveeRetroActive.setIndividu(individu);
		arriveeRetroActive.setAncienneAdressePrincipale(anciennesAdresses.principale);
		arriveeRetroActive.setAncienneCommunePrincipale(MockCommune.Lausanne);
		arriveeRetroActive.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arriveeRetroActive.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arriveeRetroActive.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		arriveeHandler.validate(arriveeRetroActive, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	@Test
	public void testGetArriveeType() throws Exception {

		// La nouvelle commune principale doit être renseignée
		{
			final MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setNouvelleCommunePrincipale(null);

			try {
				ArriveeHandler.getArriveeType(serviceInfra, arrivee);
				fail();
			}
			catch (Exception ignored) {
				// Ok
			}
		}

		// Arrivée en résidence secondaire
		{
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setAncienneCommuneSecondaire(null);
			arrivee.setNouvelleCommuneSecondaire(MockCommune.Bex);
			assertEquals(ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE, ArriveeHandler.getArriveeType(serviceInfra, arrivee));
		}

		// Arrivée en résidence principale
		{
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setNouvelleCommunePrincipale(MockCommune.RomainmotierEnvy);
			arrivee.setAncienneCommuneSecondaire(null);
			arrivee.setNouvelleCommuneSecondaire(null);
			assertEquals(ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE, ArriveeHandler.getArriveeType(serviceInfra, arrivee));

			// la commune secondaire est ignorée si la commune principale est dans le canton
			arrivee.setAncienneCommuneSecondaire(MockCommune.Neuchatel);
			arrivee.setNouvelleCommuneSecondaire(MockCommune.RomainmotierEnvy);
			assertEquals(ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE, ArriveeHandler.getArriveeType(serviceInfra, arrivee));
		}

		// Arrivée hors canton
		{
			MockArrivee arrivee = new MockArrivee();
/*			arrivee.setAncienneCommunePrincipale(null);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Neuchatel);*/
			try {
				ArriveeHandler.getArriveeType(serviceInfra, arrivee);
				fail();
			}
			catch (Exception ignored) {
				// Ok
			}
		}
	}

	/**
	 * Teste qu'une arrivée hors canton génère bien une exception.
	 */
	@Test
	public void testValidateAdressePrincipaleHorsCanton() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel, dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel, dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		/*
		 * Erreur: nouvelle adresse hors-canton
		 */
		MockArrivee arriveeHorsCanton = new MockArrivee();
		arriveeHorsCanton.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arriveeHorsCanton.setDate(dateArrivee);
		arriveeHorsCanton.setIndividu(individu);
		arriveeHorsCanton.setAncienneAdressePrincipale(anciennesAdresses.principale);
		arriveeHorsCanton.setAncienneCommunePrincipale(MockCommune.Lausanne);
		arriveeHorsCanton.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arriveeHorsCanton.setNouvelleCommunePrincipale(MockCommune.Neuchatel);
		arriveeHorsCanton.setNumeroOfsCommuneAnnonce(MockCommune.Neuchatel.getNoOFS());

		arriveeHandler.validate(arriveeHorsCanton, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	/**
	 * Teste qu'une arrivée sur une commune composée de fractions (L'Abbaye, Le Chenit et le Lieu) génère bien une exception pour
	 * insuffisance d'information (on a besoin de la fraction précise).
	 */
	@Test
	public void testValidateAdressePrincipaleCommunesComposeesFractions() throws Exception {

		final long noIndividuLAbbaye = 1;
		final long noIndividuLeChenit = 2;
		final long noIndividuLeLieu = 3;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuLAbbaye, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LePont, dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LePont, dateArrivee, null);

				MockIndividu jacques = addIndividu(noIndividuLeChenit, RegDate.get(1970, 4, 20), "Magnenat", "Jacques", true);

				// adresses avant l'arrivée
				addAdresse(jacques, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LePont, RegDate.get(1988, 3, 7), veilleArrivee);
				addAdresse(jacques, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LePont, RegDate.get(1988, 3, 7), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jacques, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LeSentier, dateArrivee, null);
				addAdresse(jacques, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LeSentier, dateArrivee, null);

				MockIndividu jean = addIndividu(noIndividuLeLieu, RegDate.get(1952, 1, 23), "Meylan", "Jean", true);

				// adresses avant l'arrivée
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LeSentier, RegDate.get(1952, 1, 23), veilleArrivee);
				addAdresse(jean, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LeSentier, RegDate.get(1952, 1, 23), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LesCharbonnieres, dateArrivee, null);
				addAdresse(jean, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LesCharbonnieres, dateArrivee, null);
			}
		});

		// Crée les habitants correspondant
		{
			PersonnePhysique pierre = newHabitant(noIndividuLAbbaye);
			PersonnePhysique jacques = newHabitant(noIndividuLeChenit);
			PersonnePhysique jean = newHabitant(noIndividuLeLieu);
			tiersDAO.save(pierre);
			tiersDAO.save(jacques);
			tiersDAO.save(jean);
		}

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		/*
		 * Erreur: nouvelle adresse sur la commune composée du Chenit
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLAbbaye, dateArrivee.year());
			final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
			final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Fraction.LeSentier); // erreur: devrait être MockCommune.Fraction.LAbbaye ou ...
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.LeChenit.getNoOFS());

			arriveeHandler.validate(arrivee, erreurs, warnings);
			assertTrue(erreurs.isEmpty());
			assertEquals(1, warnings.size());
			assertEquals("arrivée dans la fraction de commune du Sentier: veuillez vérifier la fraction de commune du for principal",
					warnings.get(0).getMessage());
			erreurs.clear();
			warnings.clear();
		}

		/*
		 * Ok: nouvelle adresse sur la commune du Lieu (qui anciennement était une commune avec fractions mais qui ne l'est plus)
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeLieu, dateArrivee.year());
			final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
			final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Fraction.LeSentier);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.LeLieu); // erreur: devrait être MockCommune.Fraction.LeLieu
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.LeLieu.getNoOFS());

			arriveeHandler.validate(arrivee, erreurs, warnings);
			assertTrue(erreurs.isEmpty());
			assertTrue(warnings.isEmpty());
			erreurs.clear();
			warnings.clear();
		}
	}

	/**
	 * Teste qu'une arrivée sur les fractions de communes du Sentier et du Lieu génère bien une exception pour traitement manuel.
	 */
	@Test
	public void testValidateAdressePrincipaleFractionsSentierEtLieu() throws Exception {

		final long noIndividuLeSentier = 1;
		final long noIndividuLeLieu = 2;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuLeSentier, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LeSentier, dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LeSentier, dateArrivee, null);

				MockIndividu jean = addIndividu(noIndividuLeLieu, RegDate.get(1952, 1, 23), "Meylan", "Jean", true);

				// adresses avant l'arrivée
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LePont, RegDate.get(1952, 1, 23), veilleArrivee);
				addAdresse(jean, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LePont, RegDate.get(1952, 1, 23), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.LeLieu, dateArrivee, null);
				addAdresse(jean, EnumTypeAdresse.COURRIER, null, null, MockLocalite.LeLieu, dateArrivee, null);
			}
		});

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		/*
		 * Erreur: nouvelle adresse sur la commune du Chenit
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeSentier, dateArrivee.year());
			final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
			final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.LeChenit);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.LeChenit.getNoOFS());

			arriveeHandler.validate(arrivee, erreurs, warnings);
			assertFalse(erreurs.isEmpty());
			assertTrue(warnings.isEmpty());
			erreurs.clear();
			warnings.clear();
		}

		/*
		 * Erreur: nouvelle adresse sur la commune du Lieu
		 */
		{
			final Individu individu = serviceCivil.getIndividu(noIndividuLeLieu, dateArrivee.year());
			final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
			final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.LAbbaye);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.LeLieu);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.LeLieu.getNoOFS());

			arriveeHandler.validate(arrivee, erreurs, warnings);
			assertFalse(erreurs.isEmpty());
			assertTrue(warnings.isEmpty());
			erreurs.clear();
			warnings.clear();
		}
	}

	@Test
	public void testValidateAdresseSecondaire() throws Exception {

		final RegDate toutDebut = RegDate.get(1980, 2, 2);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veille = dateArrivee.getOneDayBefore();

		/*
		 * Ok: arrivée en adresse secondaire
		 */
		Adresse ancienne = MockServiceCivil.newAdresse(EnumTypeAdresse.SECONDAIRE, null, null, MockLocalite.Neuchatel, toutDebut,
				veille);
		Adresse nouvelle = MockServiceCivil.newAdresse(EnumTypeAdresse.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null,
				MockLocalite.Lausanne, dateArrivee, null);

		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setAncienneAdresseSecondaire(ancienne);
		arrivee.setNouvelleAdresseSecondaire(nouvelle);
		arrivee.setAncienneCommuneSecondaire(null);
		arrivee.setNouvelleCommuneSecondaire(MockCommune.Lausanne);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		ArriveeHandler.validateArriveeAdresseSecondaire(serviceInfra, arrivee, erreurs);
		assertTrue(erreurs.isEmpty());
		erreurs.clear();

		/*
		 * Erreur: arrivée avant le début de validité de l'adresse principale
		 */
		MockArrivee arriveeRetroActive = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arriveeRetroActive.setDate(RegDate.get(1902, 10, 11));
		arriveeRetroActive.setAncienneAdresseSecondaire(ancienne);
		arriveeRetroActive.setNouvelleAdresseSecondaire(nouvelle);
		arriveeRetroActive.setAncienneCommuneSecondaire(null);
		arriveeRetroActive.setNouvelleCommuneSecondaire(MockCommune.Lausanne);
		arriveeRetroActive.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

		ArriveeHandler.validateArriveeAdresseSecondaire(serviceInfra, arriveeRetroActive, erreurs);
		assertFalse(erreurs.isEmpty());
		erreurs.clear();

		/*
		 * Erreur: arrivée en adresse secondaire hors canton
		 */
		Adresse nouvelleHorsCanton = MockServiceCivil.newAdresse(EnumTypeAdresse.SECONDAIRE, null, null,
				MockLocalite.Neuchatel3Serrieres, dateArrivee, null);

		MockArrivee arriveeHorsCanton = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arriveeHorsCanton.setDate(dateArrivee);
		arriveeHorsCanton.setAncienneAdresseSecondaire(ancienne);
		arriveeHorsCanton.setNouvelleAdresseSecondaire(nouvelleHorsCanton);
		arriveeHorsCanton.setAncienneCommuneSecondaire(null);
		arriveeHorsCanton.setNouvelleCommuneSecondaire(MockCommune.Neuchatel);
		arriveeHorsCanton.setNumeroOfsCommuneAnnonce(MockCommune.Neuchatel.getNoOFS());

		ArriveeHandler.validateArriveeAdresseSecondaire(serviceInfra, arriveeHorsCanton, erreurs);
		assertFalse(erreurs.isEmpty());
		erreurs.clear();
	}

	@Test
	public void testValidateIndividuSeul() throws Exception {

		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);

			tiersDAO.save(habitant);
		}

		/*
		 * Ok : événement d'arrivée standard
		 */
		MockArrivee arrivee = new MockArrivee();
		arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		arrivee.setDate(dateArrivee);
		arrivee.setIndividu(individu);
		arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
		arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
		arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
		arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
		arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

		arriveeHandler.validate(arrivee, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul suisse depuis une commune hors-canton.
	 */
	@Test
	public void testHandleArriveePrincipaleIndividuSeulSuisseDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				addNationalite(pierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null, 0);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul suisse depuis une commune du canton.
	 */
	@Test
	public void testHandleArriveePrincipaleIndividuSeulSuisseDeCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

		}

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			List<ForFiscal> list = habitant.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul étranger depuis une commune hors-canton.
	 */
	@Test
	public void testHandleArriveePrincipaleIndividuSeulEtrangerDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				addOrigine(pierre, MockPays.France, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, 0, false);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(ModeImposition.SOURCE, ff.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul réfugié depuis une commune hors-canton.
	 */
	@Test
	public void testHandleArriveePrincipaleIndividuSeulRefugieDeHorsCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				addOrigine(pierre, MockPays.France, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ANNUEL, RegDate.get(1963, 8, 20), null, 2, false);
				addPermis(pierre, EnumTypePermis.REQUERANT_ASILE_AVANT_DECISION, RegDate.get(1963, 1, 15), RegDate.get(1963, 8, 19), 1, false);
				addPermis(pierre, EnumTypePermis.REQUERANT_ASILE_REFUSE, RegDate.get(1962, 9, 2), RegDate.get(1963, 1, 14), 0, false);
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Vérification que l'individu n'existe pas en base avant son arrivée
			 */
			assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(ModeImposition.ORDINAIRE, ff.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un individu seul étranger depuis une commune du canton.
	 */
	@Test
	public void testHandleArriveePrincipaleIndividuSeulEtrangerDeCanton() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				addOrigine(pierre, MockPays.France, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, 0, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setModeImposition(ModeImposition.SOURCE);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));

		}

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			List<ForFiscal> list = habitant.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());
			assertEquals(ModeImposition.SOURCE, lausanne.getModeImposition());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
			assertEquals(ModeImposition.SOURCE, cossonay.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple depuis une commune hors-canton.
	 */
	@Test
	public void testHandleArriveePrincipaleCoupleDeHorsCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);

				// nationalité
				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);
				addOrigine(julie, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(julie, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individuPrincipal);
				arrivee.setConjoint(individuConjoint);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

				/*
				 * Vérification que les individus n'existent pas en base avant leurs arrivées
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal));
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint));

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 1, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(menage.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			assertNull(habitantPrincipal.getForFiscalPrincipalAt(null));
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));
			ForFiscalPrincipal ff = menage.getForFiscalPrincipalAt(null);
			assertNotNull(ff);

			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple depuis une commune du canton.
	 */
	@Test
	public void testHandleArriveePrincipaleCoupleDeCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);
				addOrigine(julie, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(julie, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal),
						newHabitant(noIndividuConjoint), dateArriveInitiale, null);

				MenageCommun menage = ensemble.getMenage();
				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, dateArriveInitiale, null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individuPrincipal);
				arrivee.setConjoint(individuConjoint);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			final PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantConjoint, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(menage.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));
			assertNull(habitantConjoint.getForFiscalPrincipalAt(null));

			final List<ForFiscal> list = menage.getForsFiscauxSorted();
			final ForFiscalPrincipal lausanne = (ForFiscalPrincipal) list.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArriveInitiale, lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());

			final ForFiscalPrincipal cossonay = (ForFiscalPrincipal) list.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
		}
	}

	/**
	 * Test de l'arrivée en adresse principale dans le canton d'un couple étranger depuis une commune du canton.
	 */
	@Test
	public void testHandleArriveePrincipaleCoupleEtrangerDeCanton() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(1990, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockPays.France, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, 0, false);
				addOrigine(julie, MockPays.France, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(julie, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(julie, EnumTypePermis.FRONTALIER, RegDate.get(1963, 8, 20), null, 0, false);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création de l'habitant et de sa situation avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal),
						newHabitant(noIndividuConjoint), dateArriveInitiale, null);

				MenageCommun menage = ensemble.getMenage();
				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				f.setModeImposition(ModeImposition.SOURCE);
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individuPrincipal);
				arrivee.setConjoint(individuConjoint);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			assertEquals(new Long(noIndividuPrincipal), habitant.getNumeroIndividu());
			PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitant, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitant.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitant.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);
			List<ForFiscal> listMenage = menage.getForsFiscauxSorted();
			ForFiscalPrincipal lausanne = (ForFiscalPrincipal) listMenage.get(0);
			assertNotNull(lausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), lausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(RegDate.get(1980, 1, 1), lausanne.getDateDebut());
			assertEquals(veilleArrivee, lausanne.getDateFin());
			assertEquals(ModeImposition.SOURCE, lausanne.getModeImposition());

			ForFiscalPrincipal cossonay = (ForFiscalPrincipal) listMenage.get(1);
			assertNotNull(cossonay);
			assertEquals(new Integer(MockCommune.Cossonay.getNoOFSEtendu()), cossonay.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, cossonay.getDateDebut());
			assertNull(null, cossonay.getDateFin());
			assertEquals(ModeImposition.SOURCE, cossonay.getModeImposition());
		}
	}

	/**
	 * Test de l'arrivée d'un individu seul avec une adresse fiscale courrier surchargée.
	 */
	@Test
	public void testHandleArriveeIndividuAvecAdresseFiscale() throws Exception {
		final long noIndividu = 1;
		final RegDate dateArrivee = RegDate.get(2004, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, RegDate.get(
						1980, 1, 1), veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);

		{
			/*
			 * Création de l'habitant et de sa situation avant l'arrivée
			 */
			PersonnePhysique habitant = newHabitant(noIndividu);
			ForFiscalPrincipal f = addForPrincipal(habitant, MockCommune.Lausanne, RegDate.get(1980, 1, 1), null);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			{
				AdresseSuisse adresse = new AdresseSuisse();
				adresse.setDateDebut(RegDate.get(2000, 3, 20));
				adresse.setDateFin(null);
				adresse.setPermanente(true);
				adresse.setUsage(TypeAdresseTiers.COURRIER);
				adresse.setNumeroMaison("3");
				adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				habitant.addAdresseTiers(adresse);
			}

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			assertEquals(habitant, tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu));
		}

		{
			/*
			 * L'événement d'arrivée
			 */
			MockArrivee arrivee = new MockArrivee();
			arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
			arrivee.setDate(dateArrivee);
			arrivee.setIndividu(individu);
			arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
			arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
			arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
			arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
			arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

			/*
			 * Arrivée
			 */
			arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
		}

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 1, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique habitant = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertEquals(new Long(noIndividu), habitant.getNumeroIndividu());

			/*
			 * Tests sur les adresses
			 */
			final Set<AdresseTiers> adressesTiers = habitant.getAdressesTiers();
			assertNotNull(adressesTiers);
			assertEquals(1, adressesTiers.size());
			AdresseTiers adresseCourrier = (AdresseTiers) adressesTiers.toArray()[0];
			assertNotNull(adresseCourrier);
			assertEquals(RegDate.get(2000, 03, 20), adresseCourrier.getDateDebut());
			assertNull(adresseCourrier.getDateFin()); // l'adresse courrier ne doit pas être fermée
		}
	}

	/**
	 * Test de l'arrivée d'un couple avec une adresse fiscale courrier surchargée.
	 */
	@Test
	public void testHandleArriveeCoupleAvecAdresseFiscale() throws Exception {
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateArrivee = RegDate.get(2004, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, RegDate.get(1957, 4, 19), "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateArriveInitiale, veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null, MockLocalite.CossonayVille,
						dateArrivee, null);

				// nationalité
				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);
				addOrigine(julie, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(julie, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		});

		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(newHabitant(noIndividuPrincipal),
						newHabitant(noIndividuConjoint), dateArriveInitiale, null);
				MenageCommun menage = ensemble.getMenage();

				ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Lausanne, dateArriveInitiale, null);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				{
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setDateDebut(RegDate.get(2000, 3, 20));
					adresse.setDateFin(null);
					adresse.setPermanente(true);
					adresse.setUsage(TypeAdresseTiers.COURRIER);
					adresse.setNumeroMaison("3");
					adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
					adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
					menage.addAdresseTiers(adresse);
				}
				{
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setDateDebut(RegDate.get(2000, 3, 20));
					adresse.setDateFin(null);
					adresse.setPermanente(false);
					adresse.setUsage(TypeAdresseTiers.DOMICILE);
					adresse.setNumeroMaison("3");
					adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
					adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
					menage.addAdresseTiers(adresse);
				}

				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individuPrincipal);
				arrivee.setConjoint(individuConjoint);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Cossonay);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Cossonay.getNoOFS());

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 2, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}

		{
			/*
			 * Vérification du couple
			 */
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal);
			final PersonnePhysique habitantConjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint);
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantConjoint, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée
			assertEmpty(habitantConjoint.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			final Set<AdresseTiers> adressesTiers = menage.getAdressesTiers();
			assertNotNull(adressesTiers);
			assertEquals(2, adressesTiers.size());
			AdresseTiers adresse1 = (AdresseTiers) adressesTiers.toArray()[0];
			AdresseTiers adresse2 = (AdresseSuisse) adressesTiers.toArray()[1];
			assertNotNull(adresse1);
			assertNotNull(adresse2);
			AdresseTiers adresseCourrier = null;
			AdresseTiers adresseDomicile = null;
			if (adresse1.getUsage().equals(TypeAdresseTiers.COURRIER)) {
				adresseCourrier = adresse1;
				adresseDomicile = adresse2;
			}
			else {
				adresseCourrier = adresse2;
				adresseDomicile = adresse1;
				assertEquals(TypeAdresseTiers.DOMICILE, adresse1.getUsage());
			}
			assertEquals(RegDate.get(2000, 03, 20), adresseCourrier.getDateDebut());
			assertNull(adresseCourrier.getDateFin()); // l'adresse courrier ne doit pas être fermée car elle est permanante
			assertNotNull(adresseDomicile.getDateFin()); // l'adresse domicile doit être fermée car elle est temporaire
		}
	}

	/**
	 * Test de l'arrivée hors-canton d'un individu avec recherche du non-habitant correspondant dans unireg.
	 */
	@Test
	public void testHandleArriveeIndividuAvecNonHabitantDeHorsCanton() throws Exception {
		
		final RegDate dateArrivee = RegDate.get(2007, 11, 19);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();
		final long numeroIndividu = 254879;
		long numeroCTB;
		
		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = RegDate.get(1987, 5, 1);
				
				MockIndividu bea = addIndividu(numeroIndividu, dateNaissance, "Duval", "Béatrice", false);
				
				// adresses avant l'arrivée
				addAdresse(bea, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(bea, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(bea, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(bea, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				
				addOrigine(bea, MockPays.Suisse, MockCommune.Lausanne, dateNaissance);
				addNationalite(bea, MockPays.Suisse, dateNaissance, null, 0);
			}
		});
		
		final Individu individu = serviceCivil.getIndividu(numeroIndividu, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individu.getNoTechnique(), dateArrivee);
		
		numeroCTB = ((PersonnePhysique) doInNewTransaction(new TxCallback() {
			@SuppressWarnings("deprecation")
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création du non-habitant
				 */
				final RegDate dateNaissanceBea = RegDate.get(1987, 5, 1);
				
				PersonnePhysique nonHabitant = newNonHabitant("Duval", "Béatrice", dateNaissanceBea, Sexe.FEMININ);
				
				// for principal
				{
					final ForFiscalPrincipal f = addForPrincipal(nonHabitant, MockCommune.Bern, dateArrivee.addYears(-1), null);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
					f.setMotifOuverture(MotifFor.INDETERMINE);
					f.setMotifRattachement(MotifRattachement.DOMICILE);
				}
				// for secondaire
				{
					final ForFiscalSecondaire f = addForSecondaire(nonHabitant, MockCommune.Lausanne, dateArrivee.addYears(-1), null);
					f.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
					f.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
				}
				nonHabitant = (PersonnePhysique) tiersDAO.save(nonHabitant);
				
				return nonHabitant;
			}

		})).getNumero();
		
		/*
		 * Indexation des données fiscales
		 */
		indexData();
		
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individu);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

				/*
				 * Vérification que l'individu n'existe pas en base avant son arrivée
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu));

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				
				return null;
			}
		});
		
		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 1, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 0, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 3, tiersDAO.getCount(ForFiscal.class));
		}
		
		{
			PersonnePhysique habitant = tiersDAO.getPPByNumeroIndividu(numeroIndividu);
			assertNotNull(habitant);
			assertTrue(habitant.isHabitant());
			assertEquals(new Long(numeroIndividu), habitant.getNumeroIndividu());
			assertEquals(new Long(numeroCTB), habitant.getNumero());

			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitant.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			ForFiscalPrincipal ff = habitant.getForFiscalPrincipalAt(null);
			assertNotNull(ff);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), ff.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, ff.getDateDebut());
			assertNull(null, ff.getDateFin());
		}
		
	}
	
	/**
	 * Test de l'arrivée hors-canton d'un individu avec recherche du non-habitant correspondant dans unireg.
	 */
	@Test
	public void testHandleArriveeCoupleNonHabitantsDeHorsCanton() throws Exception {
		
		final long noIndividuPrincipal = 1;
		final long noIndividuConjoint = 2;
		final RegDate dateArriveInitiale = RegDate.get(1980, 1, 1);
		final RegDate dateDepart = RegDate.get(2005, 1, 1);
		final RegDate dateArrivee = RegDate.get(2008, 7, 1);
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		long noCTBPrincipal;
		long noCTBConjoint;
		
		final RegDate dateNaissancePierre = RegDate.get(1953, 11, 2);
		final RegDate dateNaissanceJulie = RegDate.get(1957, 4, 19);
		
		/*
		 * Création des données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, dateNaissancePierre, "Dupont", "Pierre", true);
				MockIndividu julie = addIndividu(noIndividuConjoint, dateNaissanceJulie, "Goux", "Julie", false);

				// adresses avant l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);
				addAdresse(julie, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Neuchatel1Cases, RegDate.get(1980, 1, 1),
						veilleArrivee);

				// adresses après l'arrivée
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);
				addAdresse(julie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateArrivee,
						null);

				// nationalité
				addOrigine(pierre, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(pierre, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(pierre, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);
				addOrigine(julie, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1963, 8, 20));
				addNationalite(julie, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addPermis(julie, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

				// marie les individus
				marieIndividus(pierre, julie, RegDate.get(1985, 07, 11));
			}
		});
		
		final Individu individuPrincipal = serviceCivil.getIndividu(noIndividuPrincipal, dateArrivee.year());
		final Individu individuConjoint = serviceCivil.getIndividu(noIndividuConjoint, dateArrivee.year());
		final AdressesCiviles anciennesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), veilleArrivee);
		final AdressesCiviles nouvellesAdresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), dateArrivee);
		
		final class Couple {
			final long idPrincipal;
			final long idConjoint;
			
			public Couple(long idPrincipal, long idConjoint) {
				this.idPrincipal = idPrincipal;
				this.idConjoint = idConjoint;
			}
		}
		
		Couple coupleContribuables = (Couple) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * Création des non-habitants
				 */
				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.save(newNonHabitant("Dupont", "Pierre", dateNaissancePierre, Sexe.MASCULIN));
				
				final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.save(newNonHabitant("Goux", "Julie", dateNaissanceJulie, Sexe.FEMININ));
				
				/*
				 * Création des habitants et de leurs situations avant l'arrivée
				 */
				final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(principal, conjoint, dateArriveInitiale, dateDepart);

				MenageCommun menage = ensemble.getMenage();
				final ForFiscalPrincipal f = addForPrincipal(menage, MockCommune.Vevey, dateArriveInitiale, dateDepart);
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				f.setMotifFermeture(MotifFor.DEPART_HC);
				
				menage = (MenageCommun) tiersDAO.save(menage);
				assertNotNull(menage);
				
				return new Couple(principal.getNumero(), conjoint.getNumero());
			}

		});
		
		assertNotNull(coupleContribuables);
		noCTBPrincipal = coupleContribuables.idPrincipal;
		noCTBConjoint = coupleContribuables.idConjoint;
		
		/*
		 * Indexation des données fiscales
		 */
		indexData();
		
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				/*
				 * L'événement d'arrivée
				 */
				MockArrivee arrivee = new MockArrivee();
				arrivee.setType(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
				arrivee.setDate(dateArrivee);
				arrivee.setIndividu(individuPrincipal);
				arrivee.setConjoint(individuConjoint);
				arrivee.setAncienneAdressePrincipale(anciennesAdresses.principale);
				arrivee.setAncienneCommunePrincipale(MockCommune.Neuchatel);
				arrivee.setNouvelleAdressePrincipale(nouvellesAdresses.principale);
				arrivee.setNouvelleCommunePrincipale(MockCommune.Lausanne);
				arrivee.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());

				/*
				 * Vérification que les individus n'existent pas en base avant leur arrivée
				 */
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPrincipal));
				assertNull(tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuConjoint));

				/*
				 * Arrivée
				 */
				arriveeHandler.handle(arrivee, new ArrayList<EvenementCivilErreur>());
				
				return null;
			}
		});
		
		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 4, tiersDAO.getCount(RapportEntreTiers.class));
			assertEquals("Nombre d'adresses incorrect", 0, tiersDAO.getCount(AdresseTiers.class));
			assertEquals("Nombre de fors fiscaux", 2, tiersDAO.getCount(ForFiscal.class));
		}
		
		{
			PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(noIndividuPrincipal);
			assertNotNull(habitantPrincipal);
			assertTrue(habitantPrincipal.isHabitant());
			assertEquals(new Long(noIndividuPrincipal), habitantPrincipal.getNumeroIndividu());
			assertEquals(new Long(noCTBPrincipal), habitantPrincipal.getNumero());

			PersonnePhysique habitantConjoint = tiersDAO.getPPByNumeroIndividu(noIndividuConjoint);
			assertNotNull(habitantConjoint);
			assertTrue(habitantConjoint.isHabitant());
			assertEquals(new Long(noIndividuConjoint), habitantConjoint.getNumeroIndividu());
			assertEquals(new Long(noCTBConjoint), habitantConjoint.getNumero());
			
			/*
			 * Tests sur les adresses
			 */
			assertEmpty(habitantPrincipal.getAdressesTiers()); // aucune adresse fiscal surchargée ne doit être créée

			/*
			 * Tests sur les fors fiscaux
			 */
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, dateArrivee);
			assertNotNull(couple);
			assertTrue(((habitantPrincipal.equals(couple.getPrincipal())) && (habitantConjoint.equals(couple.getConjoint())))
					|| ((habitantPrincipal.equals(couple.getConjoint())) && (habitantConjoint.equals(couple.getPrincipal()))));
			final MenageCommun menage = couple.getMenage();
			assertNotNull(menage);
			
			List<ForFiscal> listForsMenage = menage.getForsFiscauxSorted();
			
			ForFiscalPrincipal forVevey = (ForFiscalPrincipal) listForsMenage.get(0);
			assertNotNull(forVevey);
			assertEquals(new Integer(MockCommune.Vevey.getNoOFSEtendu()), forVevey.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArriveInitiale, forVevey.getDateDebut());
			assertEquals(dateDepart, forVevey.getDateFin());

			ForFiscalPrincipal forLausanne = (ForFiscalPrincipal) listForsMenage.get(1);
			assertNotNull(forLausanne);
			assertEquals(new Integer(MockCommune.Lausanne.getNoOFSEtendu()), forLausanne.getNumeroOfsAutoriteFiscale());
			assertEquals(dateArrivee, forLausanne.getDateDebut());
			assertNull(null, forLausanne.getDateFin());
		}
		
	}
	
	private static PersonnePhysique newHabitant(long noIndividuPrincipal) throws Exception {

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividuPrincipal);
		return habitant;
	}

	private static PersonnePhysique newNonHabitant(String nom, String prenom, RegDate dateNaissance, Sexe sexe) {
		PersonnePhysique nonHabitant = new PersonnePhysique(false);
		nonHabitant.setPrenom(prenom);
		nonHabitant.setNom(nom);
		nonHabitant.setSexe(sexe);
		nonHabitant.setDateNaissance(dateNaissance);
		return nonHabitant;
	}
	
	private static ForFiscalPrincipal addForPrincipal(Contribuable contribuable, Commune commune, RegDate ouverture, RegDate fermeture) {
		final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		final ForFiscalPrincipal ffp = new ForFiscalPrincipal(ouverture, fermeture, commune.getNoOFS(), type, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		contribuable.addForFiscal(ffp);
		return ffp;
	}
	
	private static ForFiscalSecondaire addForSecondaire(Contribuable contribuable, Commune commune, RegDate ouverture, RegDate fermeture) {
		final ForFiscalSecondaire ffs = new ForFiscalSecondaire(ouverture, fermeture, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
		contribuable.addForFiscal(ffs);
		return ffs;
	}
}
