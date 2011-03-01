package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Test du handler de suppression de nationalité suisse:
 * -----------------------------------------------------
 *
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteHandlerTest extends AbstractEvenementHandlerTest {

	private static final Log LOGGER = LogFactory.getLog(SuppressionNationaliteHandlerTest.class);

	/**
	 * Le numéro d'individu du celibataire.
	 */
	private static final Long NO_INDIVIDU_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final Long NO_INDIVIDU_MARIE_SEUL = 12345L;

	/**
	 * Le numéro d'individu du marié à deux.
	 */
	private static final Long NO_INDIVIDU_MARIE = 54321L;
	private static final Long NO_INDIVIDU_MARIE_CONJOINT = 23456L;

	/**
	 * Les dates d'obtention de la nationalité
	 */
	private static final RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(2008, 9, 8);

	/**
	 * Le fichier de données de test. C'est le meme que pour l'annulation de
	 * permis, les règles de traitement similiaires.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationPermisTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				MockIndividu julie = getIndividu(NO_INDIVIDU_CELIBATAIRE);
				addOrigine(julie, MockPays.France, null, RegDate.get(1976, 4, 19));
				addNationalite(julie, MockPays.France, RegDate.get(1976, 4, 19), null, 0);
				addNationalite(julie, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null, 0);
			}
		});
		loadDatabase(DB_UNIT_DATA_FILE);

	}

	@Test
	public void testAnnulationNationaliteHandlerCelibataire() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de nationalité de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, 2008);
		SuppressionNationalite annulationNationalite = createValidAnnulationNationalite(celibataire, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		annulationNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation.", erreurs);

		annulationNationalite.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.", erreurs);

		annulationNationalite.handle(warnings);

		 // Test de récupération du Tiers
		PersonnePhysique julie = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_CELIBATAIRE);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				julie);

		// Vérification des fors fiscaux
		assertNotNull("Les for fiscaux de Julie ont disparus",
				julie.getForsFiscaux());
		assertNotNull("Julie devrait encore avoir un for principal actif après l'annulation de la nationalité suisse",
				julie.getForFiscalPrincipalAt(null));
	}

	@Test
	public void testAnnulationNationaliteHandlerCelibataireNonSuisse() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de nationalité non suisse de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, 2008);
		SuppressionNationalite annulationNationalite = createValidAnnulationNationatieNonSuisse(celibataire, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		annulationNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation.",
				erreurs);

		annulationNationalite.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.",
				erreurs);

		annulationNationalite.handle(warnings);

		// Test de récupération du Tiers
		PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_CELIBATAIRE);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				julie);

		/*
		 * Vérification des fors fiscaux
		 */
		assertNotNull("Les for fiscaux de Julie ont disparus",
				julie.getForsFiscaux());
		assertNotNull("Julie devrait encore avoir un for principal actif après l'annulation de nationalité non suisse",
				julie.getForFiscalPrincipalAt(null));
	}

	@Test
	public void testAnnulationNationaliteHandlerMarieSeul() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de nationalité de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, 2008);
		SuppressionNationalite annulationNationalite = createValidAnnulationNationalite(marieSeul, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		annulationNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation.",
				erreurs);

		annulationNationalite.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.",
				erreurs);

		annulationNationalite.handle(warnings);

		// Test de récupération du Tiers
		PersonnePhysique pierre = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				pierre);

		// Vérification des fors fiscaux
		assertEquals("Les for fiscaux de Pierre ont disparus", 1, pierre.getForsFiscaux().size());
		assertNull("Pierre ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse",
				pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",
				forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)",
				forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)",
				forCommun.getMotifFermeture());
	}

	@Test
	public void testAnnulationNationaliteHandlerMarieADeux() {

		LOGGER.debug("Test de traitement d'un événement d'annulation de nationalité de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, 2007);
		SuppressionNationalite annulationNationalite = createValidAnnulationNationalite(marieADeux, DATE_OBTENTION_NATIONALITE);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		annulationNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation.",
				erreurs);

		annulationNationalite.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation.",
				erreurs);

		annulationNationalite.handle(warnings);

		// Test de récupération du Tiers
		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)",
				momo);

		// Vérification des fors fiscaux
		assertNull("Momo ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse",
				momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Test de récupération du Conjoint
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", bea);

		// Vérification des fors fiscaux
		assertNull("Béa ne doit toujours pas avoir de for principal actif après  l'obtention de nationalité suisse",
				bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé",
						forFiscal.getDateFin());
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",
				forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)",
				forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)",
				forCommun.getMotifFermeture());
	}

	private SuppressionNationalite createValidAnnulationNationalite(Individu individu, RegDate dateObtentionNationalite) {

		MockSuppressionNationalite annulationNationalite = new MockSuppressionNationalite();
		annulationNationalite.setIndividu(individu);
		annulationNationalite.setNumeroOfsCommuneAnnonce(5586);
		annulationNationalite.setDate(dateObtentionNationalite);
		annulationNationalite.setNationaliteSuisse(true);
		annulationNationalite.setType(TypeEvenementCivil.SUP_NATIONALITE_SUISSE);
		annulationNationalite.setHandler(evenementCivilHandler);
		return annulationNationalite;
	}

	private SuppressionNationalite createValidAnnulationNationatieNonSuisse(Individu individu, RegDate dateObtentionNationalite) {
		MockSuppressionNationalite annulationNationalite = new MockSuppressionNationalite();
		annulationNationalite.setIndividu(individu);
		annulationNationalite.setNumeroOfsCommuneAnnonce(5586);
		annulationNationalite.setDate(dateObtentionNationalite);
		annulationNationalite.setNationaliteSuisse(false);
		annulationNationalite.setType(TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE);
		annulationNationalite.setHandler(evenementCivilHandler);
		return annulationNationalite;
	}

}
