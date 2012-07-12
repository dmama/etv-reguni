package ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
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
import static org.junit.Assert.assertTrue;

public class AnnulationReconciliationTest extends AbstractEvenementCivilInterneTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationReconciliationTest.class);
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationReconciliationTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationReconciliationMarieSeul() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de réconciliation d'une personne mariée seule");
		
		final RegDate dateReconciliation = RegDate.get(2000, 11, 3);
		final long noIndividu = 12345;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
		AnnulationReconciliation annulation = createAnnulationReconciliation(individu, dateReconciliation);
		
		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de réconciliation", collector.getErreurs());
		annulation.handle(collector);
		
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(noIndividu);
		assertNotNull("Pierre n'as pas été trouvé", pierre);
		
		// Vérification des fors fiscaux
		assertNotNull("Pierre doit avoir un for principal actif après l'annulation de réconciliation", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		// Vérification de la présence d'un tiers MenageCommun annulé
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateReconciliation.equals(rapport.getDateDebut())) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNull("Aucun for fiscal principal actif aurait dû être trouvé sur le tiers MenageCommun", forCommun);
		for (ForFiscal forFiscal : menageCommun.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux du ménage créés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal de Pierre
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationReconciliationCouple() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de réconciliation d'une personne mariée et dont le conjoint est connu");
		
		final RegDate dateReconciliation = RegDate.get(2005, 7, 15);
		final long noIndividuMarie = 54321; // momo
		final long noIndividuConjoint = 23456; // béa
		
		Individu individu = serviceCivil.getIndividu(noIndividuMarie, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(noIndividuConjoint, date(2008, 12, 31));
		AnnulationReconciliation annulation = createAnnulationReconciliation(individu, conjoint, dateReconciliation);
		
		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de réconciliation", collector.getErreurs());
		annulation.handle(collector);

		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(noIndividuMarie);
		assertNotNull("Le tiers n'as pas été trouvé", momo);
		// Vérification des fors fiscaux de momo
		assertNotNull("Maurice doit avoir un for principal actif après l'annulation de réconciliation", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(noIndividuConjoint);
		assertNotNull("Le tiers n'as pas été trouvé", bea);
		// Vérification des fors fiscaux de bea
		assertNotNull("Béatrice doit avoir un for principal actif après l'annulation de réconciliation", bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		// Vérification de la présence d'un tiers MenageCommun annulé
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateReconciliation.equals(rapport.getDateDebut())) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNull("Aucun for fiscal principal actif aurait dû être trouvé sur le tiers MenageCommun", forCommun);
		for (ForFiscal forFiscal : menageCommun.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux du ménage créés lors de la réconciliation doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal de Maurice
		 *  - réouverture for fiscal principal de Béatrice
		 */
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(momo).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(bea).size());
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationMariageCelibataire() {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne non mariée (cas d'erreur).");
		
		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		final long noIndividu = 6789;
		
		Individu individu = serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
		AnnulationReconciliation annulation = createAnnulationReconciliation(individu, dateFictive);
		
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
		assertTrue("Une erreur aurait dû se produire car cette personne n'est pas réconciliée", errorFound);
		assertEquals("L'erreur n'est pas la bonne", "Le tiers ménage commun n'a pu être trouvé", errorMessage);
	}
	
	private AnnulationReconciliation createAnnulationReconciliation(Individu individu, RegDate date) {
		return new AnnulationReconciliation(individu, null, date, 5652, context);
	}

	private AnnulationReconciliation createAnnulationReconciliation(Individu individu, Individu conjoint, RegDate date) {
		return new AnnulationReconciliation(individu, conjoint, date, 5652, context);
	}
}
