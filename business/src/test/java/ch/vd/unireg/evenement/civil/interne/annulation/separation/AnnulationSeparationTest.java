package ch.vd.unireg.evenement.civil.interne.annulation.separation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AnnulationSeparationTest extends AbstractEvenementCivilInterneTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationSeparationTest.class);
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationSeparationTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockIndividuConnector());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationSeparationMarieSeul() throws EvenementCivilException {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'une personne mariée seule");
	
		final RegDate dateSeparation = date(2000, 9, 12);
		final long noIndividu = 12345;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateSeparation);
		
		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de séparation", collector.getErreurs());

		annulation.handle(collector);
		
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(noIndividu);
		assertNotNull("Pierre n'as pas été trouvé", pierre);
		checkContribuableApresAnnulation(pierre, dateSeparation);

		MenageCommun menage = checkMenageApresAnnulation(pierre, dateSeparation);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - annulation du for fermé
		 *  - réouverture for fiscal principal du ménage Pierre
		 */
		assertEquals(2, eventSender.getCount());
		assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(menage).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationSeparation() throws EvenementCivilException {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'un couple de contribuables mariés");
		
		final RegDate dateSeparation = date(2004, 3, 2);
		final long noIndividuMarie = 54321; // momo
		final long noIndividuConjoint = 23456; // béa
		
		Individu individu = serviceCivil.getIndividu(noIndividuMarie, date(2008, 12, 31));
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateSeparation);
		
		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de séparation", collector.getErreurs());
		annulation.handle(collector);
		
		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(noIndividuMarie);
		assertNotNull("Maurice n'as pas été trouvé", momo);
		checkContribuableApresAnnulation(momo, dateSeparation);
		
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(noIndividuConjoint);
		assertNotNull("Béatrice n'as pas été trouvé", bea);
		checkContribuableApresAnnulation(bea, dateSeparation);
		
		MenageCommun menage = checkMenageApresAnnulation(momo, dateSeparation);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - annulation du for fermé
		 *  - réouverture for fiscal principal du ménage Pierre
		 */
		assertEquals(2, eventSender.getCount());
		assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(menage).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationSeparationCelibataire() {
		LOGGER.debug("Test de traitement d'un événement d'annulation de séparation d'une personne non mariée (cas d'erreur).");
		
		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		final long noIndividu = 6789;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
		AnnulationSeparation annulation = createAnnulationSeparation(individu, dateFictive);
		
		final MessageCollector collector = buildMessageCollector();
		boolean errorFound = false;
		String errorMessage = null;
		try {
			annulation.validate(collector, collector);
		}
		catch (Exception ex) {
			errorFound = true;
			errorMessage = ex.getMessage();
		}
		assertTrue("Une erreur aurait dû se produire car cette personne n'est pas séparée", errorFound);
		assertEquals("L'erreur n'est pas la bonne", "Le tiers ménage commun n'a pu être trouvé", errorMessage);
	}

	private AnnulationSeparation createAnnulationSeparation(Individu individu, RegDate date) {
		return new AnnulationSeparation(individu, null, date, 5652, context);
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
