package ch.vd.uniregctb.evenement.civil.interne.separation;

import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Test du handler de séparation:
 * ------------------------------
 * 
 * @author Pavel BLANCO
 *
 */
public class Separation2Test extends AbstractEvenementCivilInterneTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Separation2Test.class);
	
	final static private RegDate DATE_SEPARATION = date(2008, 10, 10);

	// test personne mariée seule
	private static final long INDIVIDU_MARIE_SEUL = 12345;
	private static final long MARIE_SEUL_MENAGE_COMMUN = 7008;

	// test personne marié avec suisse ou permis C
	private static final long INDIVIDU_MARIE = 54321;
	private static final long INDIVIDU_MARIE_CONJOINT = 23456;
	private static final long TIERS_MENAGE_COMMUN = 7004;
	
	// test personne marié avec un habitant de nationalité inconnue 
	private static final long INDIVIDU_MARIE2 = 78912;
	private static final long INDIVIDU_MARIE2_CONJOINT = 89123;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "Separation2Test.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			
			@Override
			protected void init() {
				super.init();
				
				MockIndividu pierre = getIndividu(INDIVIDU_MARIE_SEUL);
				separeIndividu(pierre, DATE_SEPARATION);
				
				MockIndividu momo = getIndividu(INDIVIDU_MARIE);
				MockIndividu bea = getIndividu(INDIVIDU_MARIE_CONJOINT);
				separeIndividus(momo, bea, DATE_SEPARATION);
				
				MockIndividu leon = getIndividu(INDIVIDU_MARIE2);
				MockIndividu helene = getIndividu(INDIVIDU_MARIE2_CONJOINT);
				separeIndividus(leon, helene, DATE_SEPARATION);
			}
		});
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationPersonneMarieeSeule() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne mariée seule.");
		Individu marieSeul = serviceCivil.getIndividu(INDIVIDU_MARIE_SEUL, date(2008, 12, 31));
		Separation separation = createValidSeparation(marieSeul, null);
		
		final MessageCollector collector = buildMessageCollector();
		separation.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate de la séparation.", collector.hasErreurs());
		
		separation.handle(collector);
		
		PersonnePhysique habitantSepare = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE_SEUL);
		assertNotNull("le tiers correspondant au séparé n'a pas été trouvé", habitantSepare);
		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		assertNotNull("le for principal du séparé n'a pas été ouvert", habitantSepare.getForFiscalPrincipalAt(null));
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(MARIE_SEUL_MENAGE_COMMUN);
		assertNotNull("le tiers correspondant au ménage commun n'a pas été trouvé", menageCommun);
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportSepares = habitantSepare.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le tiers
		 */
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());

	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationPersonneMarieeAvecSuisseOuPermisC() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne mariée avec un suisse ou étranger avec permis C.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE_CONJOINT, date(2008, 12, 31));
		Separation separation = createValidSeparation(marie, conjoint);
		
		final MessageCollector collector = buildMessageCollector();
		separation.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate de la séparation.", collector.hasErreurs());
		
		separation.handle(collector);
		
		/*
		 * Test de récupération du tiers qui se sépare
		 */
		PersonnePhysique habitantSepare = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE);
		assertNotNull("le tiers correspondant au séparé n'a pas été trouvé", habitantSepare);
		
		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		assertNotNull("le for principal du séparé n'a pas été ouvert", habitantSepare.getForFiscalPrincipalAt(null));
		
		/*
		 * Test de récupération du conjoint
		 */
		PersonnePhysique conjointSepare = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE_CONJOINT);
		assertNotNull("le tiers correspondant au conjoint n'a pas été trouvé", conjointSepare);
		
		/*
		 * Il doit avoir un for principal ouvert et soumis au regime ordinaire
		 */
		assertNotNull("le for principal du conjoint n'a pas été ouvert", conjointSepare.getForFiscalPrincipalAt(null));
		assertEquals("le conjoint devrait être soumis à l'impôt ordinaire", conjointSepare.getForFiscalPrincipalAt(null).getModeImposition(),
				ModeImposition.ORDINAIRE);
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(TIERS_MENAGE_COMMUN);
		assertNotNull("le tiers correspondant au ménage commun n'a pas été trouvé", menageCommun);
		
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportSepares = habitantSepare.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur chacun des conjoints
		 */
		assertEquals(3, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(conjointSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationPersonneMarieeDeNationaliteInconnue() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne de nationalité inconnue.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, date(2008, 12, 31));
		Separation separation = createValidSeparation(marie, conjoint);
		
		final MessageCollector collector = buildMessageCollector();
		separation.validate(collector, collector);
		assertTrue("L'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", collector.hasErreurs());
		
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Voir JIRA UNIREG-2292
	 */
	public void testSeparationJourDuMariage() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				final MockIndividu leon = getIndividu(INDIVIDU_MARIE2);
				final MockIndividu helene = getIndividu(INDIVIDU_MARIE2_CONJOINT);
				separeIndividus(leon, helene, DATE_SEPARATION);
			}
		});

		// création des contribuables
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique leon = addHabitant(INDIVIDU_MARIE2);
				final PersonnePhysique helene = addHabitant(INDIVIDU_MARIE2_CONJOINT);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(leon, helene, DATE_SEPARATION, null);        // on les marie au fiscal le jour où ils arrivent HC (ils vont en fait se séparer)
				addForPrincipal(ensemble.getMenage(), DATE_SEPARATION, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return null;
			}
		});

		final Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, date(2008, 12, 31));
		final Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, date(2008, 12, 31));
		final Separation separation = createValidSeparation(marie, conjoint);

		final MessageCollector collector = buildMessageCollector();
		separation.validate(collector, collector);
		assertTrue("L'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", collector.hasErreurs());

		try {
			separation.handle(collector);
			Assert.fail("Le traitement de l'événement aurait dû lancer une exception");
		}
		catch (EvenementCivilException e) {
			final String message = e.getMessage();
			assertEquals(String.format("On ne peut fermer le rapport d'appartenance ménage avant sa date de début (%s)", RegDateHelper.dateToDisplayString(DATE_SEPARATION)), message);
		}
	}

	private Separation createValidSeparation(Individu individu, Individu conjoint) {
		return new Separation(individu, conjoint, DATE_SEPARATION, 5652, context);
	}

	@Test
	public void testEvenementSeparationRedondant() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateSeparation = date(2008, 11, 23);

		// création d'un ménage-commun séparé au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
				separeIndividus(monsieur, madame, dateSeparation);
			}
		});

		// création d'un ménage-commun séparé au fiscal
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(monsieur, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				addForPrincipal(madame, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Chamblon);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, dateSeparation);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						MockCommune.Echallens);
				return null;
			}
		});

		// traitement de l'événement de séparé redondant
		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Individu monsieur = serviceCivil.getIndividu(noMonsieur, dateSeparation);
				final Individu madame = serviceCivil.getIndividu(noMadame, dateSeparation);

				// la date de l'événement séparation corresponds au premier jour de non-appartenance ménage des composants (à l'inverse de la logique habituelle)
				final Separation separation = new Separation(monsieur, madame, dateSeparation.getOneDayAfter(), MockCommune.Echallens.getNoOFS(), context);

				final MessageCollector collector = buildMessageCollector();
				separation.validate(collector, collector);
				final HandleStatus etat = separation.handle(collector);

				assertEmpty(collector.getErreurs());
				assertEmpty(collector.getWarnings());
				assertEquals(HandleStatus.REDONDANT, etat);
				return null;
			}
		});

		// on s'assure que rien n'a changé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final AppartenanceMenage appartenanceMonsieur = (AppartenanceMenage) monsieur.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMonsieur);
				assertEquals(dateMariage, appartenanceMonsieur.getDateDebut());
				assertEquals(dateSeparation, appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateSeparation, appartenanceMadame.getDateFin());

				assertNull(tiersService.getEnsembleTiersCouple(madame, dateSeparation.getOneDayAfter()));
				return null;
			}
		});
	}
}
