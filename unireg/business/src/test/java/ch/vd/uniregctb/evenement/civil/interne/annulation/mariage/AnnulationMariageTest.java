package ch.vd.uniregctb.evenement.civil.interne.annulation.mariage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnnulationMariageTest extends AbstractEvenementCivilInterneTest {
	
	private static final Log LOGGER = LogFactory.getLog(AnnulationMariageTest.class);
	
	/**
	 * Le numéro d'individu du célibataire.
	 */
	private static final long NO_INDIVIDU_CELIBATAIRE = 6789;
	
	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 12345;

	/**
	 * Le numéro d'individu du marié.
	 */
	private static final long NO_INDIVIDU_MARIE = 54321;
	private static final long NO_INDIVIDU_MARIE_CONJOINT = 23456;
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationMariageTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationMariageCelibataire() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne non mariée (cas d'erreur).");
		
		final RegDate dateFictive = RegDate.get(2008, 1, 1);
		AnnulationMariage annulation = createAnnulationMariage(NO_INDIVIDU_CELIBATAIRE, dateFictive);

		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", collector.getErreurs());
		
		boolean errorFound = false;
		try {
			annulation.handle(collector);
		}
		catch (Exception ex) {
			errorFound = true;
		}
		assertTrue("Une erreur aurait dû se produire car cette personne n'est pas mariée", errorFound);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationMariageMarieSeul() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'un marié seul.");
		
		final RegDate dateMariage = RegDate.get(1986, 4, 8);
		AnnulationMariage annulation = createAnnulationMariage(NO_INDIVIDU_MARIE_SEUL, dateMariage);

		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", collector.getErreurs());

		annulation.handle(collector);
		
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Le tiers n'as pas été trouvé", pierre);
		
		// Vérification des fors fiscaux
		assertNotNull("Pierre doit avoir un for principal actif après l'annulation de mariage", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateMariage.equals(rapport.getDateDebut())) {
				nbMenagesCommuns++;
				assertEquals("Tous les rapports ménage devraient être fermés ou annulés", true, rapport.isAnnule());
			}
		}
		assertEquals("Il aurait dû y avoir 1 rapport entre tiers annulé", 1, nbMenagesCommuns);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - réouverture for fiscal principal de Pierre
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationMariageMarie() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement d'annulation de mariage d'une personne mariée.");
		
		final RegDate dateMariage = RegDate.get(1986, 4, 8);
		final AnnulationMariage annulation = createAnnulationMariage(NO_INDIVIDU_MARIE, NO_INDIVIDU_MARIE_CONJOINT, dateMariage);

		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de mariage", collector.getErreurs());

		annulation.handle(collector);

		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE);
		assertNotNull("Le tiers n'as pas été trouvé", momo);
		// Vérification des fors fiscaux de momo
		assertNotNull("Maurice doit avoir un for principal actif après l'annulation de mariage", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}
		
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_CONJOINT);
		assertNotNull("Le tiers n'as pas été trouvé", bea);
		// Vérification des fors fiscaux de bea
		assertNotNull("Béatrice doit avoir un for principal actif après l'annulation de mariage", bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals("Les fors fiscaux fermés lors du mariage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && dateMariage.equals(rapport.getDateDebut())) {
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
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(momo).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(bea).size());
	}

	private AnnulationMariage createAnnulationMariage(long noIndividu, RegDate date) {

		// il faut modifier l'individu directement dans le registre civil
		final Individu individu = annuleMariage(noIndividu);

		return new AnnulationMariage(individu, null, date, 5652, context);
	}

	private AnnulationMariage createAnnulationMariage(long noIndividu, long noConjoint, RegDate dateMariage) {
		annuleMariage(noIndividu, noConjoint);
		return createAnnulationMariage(noIndividu, dateMariage);
	}

	/**
	 * Annule le mariage sur l'individu donné par son numéro dans le registre civil (i.e. supprime l'état civil marié et le lien vers le conjoint)
	 *
	 * @param noIndividu numéro d'individu de la personne dont le mariage doit être annulé
	 * @return l'individu tel que retourné par le registre civil suite à cette annulation
	 */
	private Individu annuleMariage(long noIndividu) {
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.annuleMariage(individu);
			}
		});
		return serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
	}

	/**
	 * Annule le mariage sur l'individu donné par son numéro dans le registre civil (i.e. supprime l'état civil marié et le lien vers le conjoint)
	 *
	 * @param noIndividu numéro d'individu de la personne dont le mariage doit être annulé
	 * @param noConjoint num-rod d'individu du conjoint
	 * @return l'individu tel que retourné par le registre civil suite à cette annulation
	 */
	private Individu annuleMariage(long noIndividu, long noConjoint) {
		doModificationIndividus(noIndividu, noConjoint, new IndividusModification() {
			@Override
			public void modifyIndividus(MockIndividu individu, MockIndividu conjoint) {
				MockServiceCivil.annuleMariage(individu, conjoint);
			}
		});
		return serviceCivil.getIndividu(noIndividu, date(2008, 12, 31));
	}

}
