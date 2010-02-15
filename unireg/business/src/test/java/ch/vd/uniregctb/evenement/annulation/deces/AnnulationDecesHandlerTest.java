package ch.vd.uniregctb.evenement.annulation.deces;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class AnnulationDecesHandlerTest extends AbstractEvenementHandlerTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationDecesHandlerTest.class);

	/**
	 * Le numero d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 92647;

	/**
	 * Le numero d'individu du marié.
	 */
	private static final long NO_INDIVIDU_MARIE = 54321;
	private static final long NO_INDIVIDU_MARIE_CONJOINT = 23456;
	private static final RegDate DATE_MARIAGE = RegDate.get(1986, 4, 8);

	/**
	 * Le numero d'individu du célibataire.
	 */
	private static final long NO_INDIVIDU_CELIBATAIRE = 6789;


	/**
	 * La date de décès.
	 */
	private static final RegDate DATE_DECES = RegDate.get(2008, 1, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationDecesHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {

		final RegDate dateMariageAndre = RegDate.get(1982, 12, 4);

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, RegDate.get(1956, 2, 25), "Girard", "André", true);
				addDefaultAdressesTo(andre);
				marieIndividu(andre, dateMariageAndre);
				addOrigine(andre, MockPays.France, null, andre.getDateNaissance());
				addNationalite(andre, MockPays.France, andre.getDateNaissance(), null, 0);
				addPermis(andre, EnumTypePermis.FRONTALIER, RegDate.get(2008, 9, 8), null, 0, false);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testAnnulationDecesCelibataire() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de décès d'une personne non mariée.");

		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, 2008);
		AnnulationDeces annulation = createValidAnnulationDeces(individu);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

		evenementCivilHandler.handle(annulation, warnings);

		PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_CELIBATAIRE);
		assertNotNull("Le tiers n'a pas été trouvé", julie);

		// Vérification des fors fiscaux
		assertNotNull("Julie doit avoir un for principal actif après l'annulation de décès", julie.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : julie.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && DATE_DECES.equals(forFiscal.getDateFin())) {
				assertEquals("Les fors fiscaux fermés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal sur l'ex-défunte
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(julie).size());
	}

	@Test
	public void testAnnulationDecesMarieSeul() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de décès d'un marié seul.");

		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, 2008);
		AnnulationDeces annulation = createValidAnnulationDeces(individu);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

		evenementCivilHandler.handle(annulation, warnings);

		PersonnePhysique andre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", andre);

		// Vérification des fors fiscaux
		assertNull("André ne doit pas avoir de for principal actif après l'annulation de décès", andre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : andre.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : andre.getRapportsSujet()) {
			if (rapport.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)", forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)", forCommun.getMotifFermeture());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	@Test
	public void testAnnulationDecesMarieAvecSuisseOuPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'annulation de décès d'une personne mariée avec un suisse ou étranger avec permis C.");

		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, 2008);
		Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_CONJOINT, 2008);
		AnnulationDeces annulation = createValidAnnulationDeces(individu, conjoint);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de décès", erreurs);

		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de décès", erreurs);

		evenementCivilHandler.handle(annulation, warnings);

		/*
		 * Test de récupération du tiers defunt
		 */
		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE);
		assertNotNull("Le tiers n'a pas été trouvé", momo);

		// Vérification des fors fiscaux
		assertNull("Maurice ne doit pas avoir de for principal actif après l'annulation de décès", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
		assertNotNull("Le tiers n'a pas été trouvé", bea);

		// Vérification des fors fiscaux
		assertNull("Béatrice ne doit pas avoir de for principal actif après l'annulation de décès", bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && DATE_DECES.getOneDayAfter().equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux créés lors du décès doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (rapport.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertEquals("Le for fiscal principal du ménage n'a pas la bonne date de début", DATE_MARIAGE, forCommun.getDateDebut());
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)", forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)", forCommun.getMotifFermeture());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal sur le ménage de l'ex-défunt
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu) {
		MockAnnulationDeces annulation = new MockAnnulationDeces();
		annulation.setIndividu(individu);
		annulation.setNumeroOfsCommuneAnnonce(5652);
		annulation.setDate(DATE_DECES);
		return annulation;
	}

	private AnnulationDeces createValidAnnulationDeces(Individu individu, Individu conjoint) {
		MockAnnulationDeces annulation = (MockAnnulationDeces) createValidAnnulationDeces(individu);
		annulation.setConjointSurvivant(conjoint);
		return annulation;
	}
}
