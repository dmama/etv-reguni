package ch.vd.uniregctb.evenement.annulation.mariage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class AnnulationMariageHandlerTest extends AbstractEvenementHandlerTest {
	
	private static final Log LOGGER = LogFactory.getLog(AnnulationMariageHandlerTest.class);
	
	/**
	 * Le numero d'individu du célibataire.
	 */
	private static final long NO_INDIVIDU_CELIBATAIRE = 6789;
	
	/**
	 * Le numero d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 12345;

	/**
	 * Le numero d'individu du marié.
	 */
	private static final long NO_INDIVIDU_MARIE = 54321;
	private static final long NO_INDIVIDU_MARIE_CONJOINT = 23456;
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationMariageHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	public void testAnnulationMariageCelibataire() {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne non mariée (cas d'erreur).");
		
		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_CELIBATAIRE, 2008);
		AnnulationMariage annulation = createAnnulationMariage(individu, dateFictive);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de mariage", erreurs);
		
		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", erreurs);
		
		boolean errorFound = false;
		try {
			evenementCivilHandler.handle(annulation, warnings);
		}
		catch (Exception ex) {
			errorFound = true;
		}
		assertTrue("Une erreur aurait dû se produire car cette personne n'est pas mariée", errorFound);
	}
	
	@Test
	public void testAnnulationMariageMarieSeul() {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'un marié seul.");
		
		final RegDate dateMariage = RegDate.get(1986, 4, 8);
		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, 2008);
		AnnulationMariage annulation = createAnnulationMariage(individu, dateMariage);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de mariage", erreurs);
		
		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", erreurs);

		evenementCivilHandler.handle(annulation, warnings);
		
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Le tiers n'as pas été trouvé", pierre);
		
		// Vérification des fors fiscaux
		assertNotNull("Pierre doit avoir un for principal actif après l'annulation de mariage", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(((ForFiscalRevenuFortune) forFiscal).getMotifFermeture()))) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType()) && dateMariage.equals(rapport.getDateDebut())) {
				nbMenagesCommuns++;
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal de Pierre
		 */
		assertEquals(1, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(pierre).size());
	}
	
	@Test
	public void testAnnulationMariageMarie() {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne mariée.");
		
		final RegDate dateMariage = RegDate.get(1986, 4, 8);
		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_MARIE, 2008); // momo
		Individu conjoint = serviceCivil.getIndividu(23456, 2008); // béa
		AnnulationMariage annulation = createAnnulationMariage(individu, conjoint, dateMariage);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de mariage", erreurs);
		
		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", erreurs);

		evenementCivilHandler.handle(annulation, warnings);

		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE);
		assertNotNull("Le tiers n'as pas été trouvé", momo);
		// Vérification des fors fiscaux de momo
		assertNotNull("Maurice doit avoir un for principal actif après l'annulation de mariage", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(((ForFiscalRevenuFortune) forFiscal).getMotifFermeture()))) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
		assertNotNull("Le tiers n'as pas été trouvé", bea);
		// Vérification des fors fiscaux de bea
		assertNotNull("Béatrice doit avoir un for principal actif après l'annulation de mariage", bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(((ForFiscalRevenuFortune) forFiscal).getMotifFermeture()))) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType()) && dateMariage.equals(rapport.getDateDebut())) {
				nbMenagesCommuns++;
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal de Maurice
		 *  - réouverture for fiscal principal de Béatrice
		 */
		assertEquals(2, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(momo).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(bea).size());
	}

	private MockAnnulationMariage createAnnulationMariage(Individu individu, RegDate date) {
		MockAnnulationMariage annulation = new MockAnnulationMariage();
		annulation.setIndividu(individu);
		annulation.setNumeroOfsCommuneAnnonce(5652);
		annulation.setDate(date);

		annuleMariage(individu);
		
		return annulation;
	}

	private AnnulationMariage createAnnulationMariage(Individu individu, Individu conjoint, RegDate dateMariage) {
		MockAnnulationMariage annulation = createAnnulationMariage(individu, dateMariage);

		annuleMariage(conjoint);
		
		return annulation;
	}
	
	private void annuleMariage(Individu individu) {
		ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilAlexandre = individu.getEtatCivilCourant();
		individu.getEtatsCivils().remove(etatCivilAlexandre);
		((MockIndividu) individu).setConjoint(null);
	}

}