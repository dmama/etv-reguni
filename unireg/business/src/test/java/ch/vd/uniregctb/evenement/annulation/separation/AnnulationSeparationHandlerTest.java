package ch.vd.uniregctb.evenement.annulation.separation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class AnnulationSeparationHandlerTest extends AbstractEvenementHandlerTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationSeparationHandlerTest.class);
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationSeparationHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	public void testAnnulationSeparationMarieSeul() {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'une personne mariée seule");
	
		final RegDate dateSeparation = date(2000, 9, 12);
		final long noIndividu = 12345;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, 2008);
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateSeparation);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de séparation", erreurs);
		
		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de séparation", erreurs);

		evenementCivilHandler.handle(annulation, warnings);
		
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(noIndividu);
		assertNotNull("Pierre n'as pas été trouvé", pierre);
		checkContribuableApresAnnulation(pierre, dateSeparation);

		MenageCommun menage = checkMenageApresAnnulation(pierre, dateSeparation);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal du ménage Pierre
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menage).size());
	}

	@Test
	public void testAnnulationSeparation() {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'un couple de contribuables mariés");
		
		final RegDate dateSeparation = date(2004, 3, 2);
		final long noIndividuMarie = 54321; // momo
		final long noIndividuConjoint = 23456; // béa
		
		Individu individu = serviceCivil.getIndividu(noIndividuMarie, 2008);
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateSeparation);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de séparation", erreurs);
		
		evenementCivilHandler.validate(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de séparation", erreurs);

		evenementCivilHandler.handle(annulation, warnings);
		
		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(noIndividuMarie);
		assertNotNull("Maurice n'as pas été trouvé", momo);
		checkContribuableApresAnnulation(momo, dateSeparation);
		
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(noIndividuConjoint);
		assertNotNull("Béatrice n'as pas été trouvé", bea);
		checkContribuableApresAnnulation(bea, dateSeparation);
		
		MenageCommun menage = checkMenageApresAnnulation(momo, dateSeparation);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal du ménage Pierre
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menage).size());
	}

	@Test
	public void testAnnulationSeparationCelibataire() {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'une personne non mariée (cas d'erreur).");
		
		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		final long noIndividu = 6789;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, 2008);
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateFictive);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(annulation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de l'annulation de séparation", erreurs);
		
		boolean errorFound = false;
		String errorMessage = null;
		try {
			evenementCivilHandler.validate(annulation, erreurs, warnings);
		}
		catch (Exception ex) {
			errorFound = true;
			errorMessage = ex.getMessage();
		}
		assertTrue("Une erreur aurait dû se produire car cette personne n'est pas séparée", errorFound);
		assertEquals("L'erreur n'est pas la bonne", "Le tiers ménage commun n'a pu être trouvé", errorMessage);
	}
	
	private MockAnnulationSeparation createAnnulationSeparation(Individu individu, RegDate date) {
		MockAnnulationSeparation annulation = new MockAnnulationSeparation();
		annulation.setIndividu(individu);
		annulation.setNumeroOfsCommuneAnnonce(5652);
		annulation.setDate(date);
		return annulation;
	}
	
	private void checkContribuableApresAnnulation(Contribuable contribuable, RegDate dateSeparation) {
		// Vérification des fors fiscaux du contribuable
		int nbForAnnules = 0;
		assertNull("Le contribuable ne doit avoir aucun for principal actif après l'annulation de séparation", contribuable.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : contribuable.getForsFiscaux()) {
			if (dateSeparation.equals(forFiscal.getDateDebut()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT == ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture())) {
				assertEquals("Les fors fiscaux fermés lors de la séparation doivent êtres annulés", true, forFiscal.isAnnule());
				nbForAnnules++;
			}
		}
		assertTrue("Le contribuable doit avoir au moins un for annulé", nbForAnnules > 0);
	}
	
	private MenageCommun checkMenageApresAnnulation(Contribuable contribuable, RegDate dateSeparation) {
		// Vérification de la présence d'un tiers MenageCommun annulé
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : contribuable.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() &&
					(rapport.getDateFin() != null && dateSeparation.getOneDayBefore().equals(rapport.getDateFin()))) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Un for fiscal principal actif aurait dû être trouvé sur le tiers ménage", forCommun);
		int nbForMenageAnnules = 0;
		for (ForFiscal forFiscal : menageCommun.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateSeparation.getOneDayBefore().equals(forFiscal.getDateFin())) {
				assertEquals("Les fors fiscaux du ménage fermés lors de la séparation doivent êtres rouverts", true, forFiscal.isAnnule());
				nbForMenageAnnules++;
			}
		}
		assertTrue("Le ménage doit avoir au moins un for annulé", nbForMenageAnnules > 0);
		
		return menageCommun;
	}
}
