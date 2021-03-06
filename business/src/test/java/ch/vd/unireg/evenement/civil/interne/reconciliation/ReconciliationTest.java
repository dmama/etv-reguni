package ch.vd.unireg.evenement.civil.interne.reconciliation;

import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ReconciliationTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationTest.class);

	/**
	 * Le numéro d'individu celibataire.
	 */
	private static final Long NO_INDIVIDU_SEPARE_SEUL = 12345L;

	/**
	 * Numeros des individus marié.
	 */
	private static final Long NO_INDIVIDU_SEPARE_MARIE = 54321L;
	private static final Long NO_INDIVIDU_SEPARE_MARIE_CONJOINT = 23456L;

	/**
	 * La date de séparation
	 */
	private static final RegDate DATE_SEPARATION = RegDate.get(2007, 3, 24);

	/**
	 * La date de reconciliation
	 */
	private static final RegDate DATE_RECONCILIATION = RegDate.get(2008, 12, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ReconciliationTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockIndividuConnector());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReconciliationMarieSeul() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un individu marié seul.");
		final Reconciliation reconciliation = createReconciliation(NO_INDIVIDU_SEPARE_SEUL, null, DATE_RECONCILIATION);

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(status -> {
			reconciliation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de la réconciliation.", collector.getErreurs());

			reconciliation.handle(collector);
			return null;
		});

		PersonnePhysique habitantReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_SEUL);
		assertNotNull("Le tiers correspondant au réconcilié n'a pas été trouvé", habitantReconcilie);
		assertNull("Le for principal du réconcilié n'a pas été fermé", habitantReconcilie.getForFiscalPrincipalAt(null));

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantReconcilie, DATE_RECONCILIATION);
		assertNotNull("L'ensemble tier couple n'a pas été trouvé", ensembleTiersCouple);

		MenageCommun menage = ensembleTiersCouple.getMenage();
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menage);
		assertNotNull("Le for principal du tiers ménage n'a pas été ouvert", menage.getForFiscalPrincipalAt(null));

		ForFiscalPrincipal ffp = menage.getForFiscalPrincipalAt(DATE_RECONCILIATION);
		assertNull("Le for principal du ménage est fermmé", ffp.getDateFin());
		RapportEntreTiers[] rapports = habitantReconcilie.getRapportsSujet().toArray(new RapportEntreTiers[0]);
		Arrays.sort(rapports, (r1, r2) -> r1.getDateDebut().compareTo(r2.getDateDebut()));
		RapportEntreTiers rapportSepares = rapports[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le tiers
		 */
		assertEquals(2, eventSender.getCount());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menage).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReconciliationMariesADeux() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un couple.");
		final Reconciliation reconciliation = createReconciliation(NO_INDIVIDU_SEPARE_MARIE, NO_INDIVIDU_SEPARE_MARIE_CONJOINT, DATE_RECONCILIATION);

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(status -> {
			reconciliation.validate(collector, collector);
			assertEmpty("Une erreur est survenue lors du validate de la réconciliation.", collector.getErreurs());

			reconciliation.handle(collector);
			return null;
		});

		PersonnePhysique habitantReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_MARIE);
		assertNotNull("Le tiers correspondant au réconcilié n'a pas été trouvé", habitantReconcilie);
		assertNull("Le for principal du réconcilié n'a pas été fermé", habitantReconcilie.getForFiscalPrincipalAt(null));

		PersonnePhysique conjointReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_MARIE_CONJOINT);
		assertNotNull("Le tiers correspondant au conjoint du réconcilié n'a pas été trouvé", conjointReconcilie);
		assertNull("Le for principal conjoint n'a pas été fermé", conjointReconcilie.getForFiscalPrincipalAt(null));

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantReconcilie, DATE_RECONCILIATION);
		assertNotNull("L'ensemble tier couple n'a pas été trouvé", ensembleTiersCouple);

		MenageCommun menage = ensembleTiersCouple.getMenage();
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menage);
		assertNotNull("Le for principal du tiers ménage n'a pas été ouvert", menage.getForFiscalPrincipalAt(null));

		ForFiscalPrincipal ffp = menage.getForFiscalPrincipalAt(DATE_RECONCILIATION);
		assertNull("Le for principal du ménage est fermmé", ffp.getDateFin());
		RapportEntreTiers[] rapports = habitantReconcilie.getRapportsSujet().toArray(new RapportEntreTiers[0]);
		Arrays.sort(rapports, (r1, r2) -> r1.getDateDebut().compareTo(r2.getDateDebut()));
		RapportEntreTiers rapportSepares = rapports[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture fors fiscaux sur les tiers
		 */
		assertEquals(3, eventSender.getCount());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(conjointReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menage).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReconciliationDateFuture() throws Exception {

		RegDate DATE_RECONCILIATION_FUTURE = RegDate.get(2080, 1, 1);
		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un couple.");
		final Reconciliation reconciliation = createReconciliation(NO_INDIVIDU_SEPARE_MARIE, NO_INDIVIDU_SEPARE_MARIE_CONJOINT, DATE_RECONCILIATION_FUTURE);

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(status -> {
			reconciliation.validate(collector, collector);
			return null;
		});

		assertEquals("Il devrait y avoir exactement une erreur", 1, collector.getErreurs().size());

		final EvenementErreur erreur = collector.getErreurs().iterator().next();
		assertEquals("L'erreur n'est pas la bonne", "La date de l'événement est dans le futur", erreur.getMessage());
	}

	private Reconciliation createReconciliation(final Long noIndividu, final Long noIndividuConjoint, final RegDate date) {
		/*
		 * Simulation de séparation
		 */
		if (noIndividuConjoint == null) {
			doModificationIndividu(noIndividu, individu -> {
				MockIndividuConnector.separeIndividu(individu, DATE_SEPARATION);
				MockIndividuConnector.marieIndividu(individu, DATE_RECONCILIATION);
			});
		}
		else {
			doModificationIndividus(noIndividu, noIndividuConjoint, (individu, conjoint) -> {
				MockIndividuConnector.separeIndividus(individu, conjoint, DATE_SEPARATION);
				MockIndividuConnector.marieIndividus(individu, conjoint, DATE_RECONCILIATION);
			});
		}

		final Individu individu = serviceCivil.getIndividu(noIndividu, date);
		final Individu conjoint = (noIndividuConjoint == null ? null : serviceCivil.getIndividu(noIndividuConjoint, date));

		return new Reconciliation(individu, conjoint, date, 5586, context);
	}
}
