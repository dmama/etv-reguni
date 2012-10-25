package ch.vd.uniregctb.evenement.civil.interne.divorce;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
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
 * Test du handler de divorce:
 * ---------------------------
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(DivorceTest.class);

	final static private RegDate DATE_DIVORCE = RegDate.get(2008, 10, 10);
	final static private RegDate DATE_SEPARATION = RegDate.get(2008, 1, 28);

	// test personne celibataire
	private static final long INDIVIDU_CELIBATAIRE = 34567;
	
	// test personne mariée seule
	private static final long INDIVIDU_MARIE_SEUL = 12345;
	private static final long INDIVIDU_SEUL_MENAGE_COMMUN = 7008;

	// test personne marié avec suisse ou permis C
	private static final long INDIVIDU_MARIE = 54321;
	private static final long INDIVIDU_MARIE_CONJOINT = 23456;
	private static final long TIERS_MENAGE_COMMUN = 7004;
	
	// test personne marié avec un habitant de nationalité inconnue 
	private static final long INDIVIDU_MARIE2 = 78912;
	private static final long INDIVIDU_MARIE2_CONJOINT = 89123;
	
	// test personne séparée
	private static final long INDIVIDU_SEPARE = 6789;
	private static final long INDIVIDU_SEPARE_CONJOINT = 644863;
	
	
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "DivorceTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			
			@Override
			protected void init() {
				super.init();
				
				MockIndividu pierre = getIndividu(INDIVIDU_MARIE_SEUL);
				separeIndividu(pierre, DATE_SEPARATION);
				divorceIndividu(pierre, DATE_DIVORCE);
				
				MockIndividu momo = getIndividu(INDIVIDU_MARIE);
				MockIndividu bea = getIndividu(INDIVIDU_MARIE_CONJOINT);
				separeIndividus(momo, bea, DATE_SEPARATION);
				divorceIndividus(momo, bea, DATE_DIVORCE);
				
				MockIndividu patrice = addIndividu(INDIVIDU_SEPARE_CONJOINT, RegDate.get(1961, 3, 12), "Hubert", "Patrice", true);
				MockIndividu julie = getIndividu(INDIVIDU_SEPARE);
				
				marieIndividus(patrice, julie, RegDate.get(2001, 3, 22));
				separeIndividus(patrice, julie, DATE_SEPARATION);
				divorceIndividus(patrice, julie, DATE_DIVORCE);
			}
		});
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorceCelibataire() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne seule.");
		Individu celibataire = serviceCivil.getIndividu(INDIVIDU_CELIBATAIRE, date(2008, 12, 31));
		Divorce divorce = createValidDivorce(celibataire, null);

		final MessageCollector collector = buildMessageCollector();
		divorce.validate(collector, collector);
		assertTrue("l'événement aurait du être en erreur car personne non marié", collector.hasErreurs());
		
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorcePersonneMarieeSeule() throws EvenementCivilException {
	
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne mariée seule.");
		Individu marieSeul = serviceCivil.getIndividu(INDIVIDU_MARIE_SEUL, date(2008, 12, 31));
		Divorce divorce = createValidDivorce(marieSeul, null);
		
		final MessageCollector collector = buildMessageCollector();
		divorce.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate du divorce.", collector.hasErreurs());
		
		divorce.handle(collector);
		
		PersonnePhysique habitantDivorce = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE_SEUL);
		assertNotNull("le tiers correspondant au divorcé n'a pas été trouvé", habitantDivorce);
		
		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		assertNotNull("le for principal du divorcé n'a pas été ouvert", habitantDivorce.getForFiscalPrincipalAt(null));
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(INDIVIDU_SEUL_MENAGE_COMMUN);
		assertNotNull("le tiers correspondant au menage commun n'a pas été trouvé", menageCommun);
		
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportDivorces = habitantDivorce.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers divorcés n'a pas été clos", DATE_DIVORCE.getOneDayBefore(), rapportDivorces.getDateFin());
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le tiers
		 */
		assertEquals(2, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantDivorce).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorcePersonneMarieeAvecSuisseOuPermisC() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne mariée avec un suisse ou étranger avec permis C.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE_CONJOINT, date(2008, 12, 31));
		Divorce divorce = createValidDivorce(marie, conjoint);
		
		final MessageCollector collector = buildMessageCollector();
		divorce.validate(collector, collector);
		assertFalse("Une erreur est survenue lors du validate du divorce.", collector.hasErreurs());
		
		divorce.handle(collector);
		
		/*
		 * Test de récupération du tiers qui divorce
		 */
		PersonnePhysique habitantDivorce = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE);
		assertNotNull("le tiers correspondant au divorcé n'a pas été trouvé", habitantDivorce);
		
		/*
		 * Ses for principaux actifs doivent avoir été ouverts
		 */
		assertNotNull("le for principal du divorcé n'a pas été ouvert", habitantDivorce.getForFiscalPrincipalAt(null));
		
		/*
		 * Test de récupération du conjoint
		 */
		PersonnePhysique conjointDivorce = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE_CONJOINT);
		assertNotNull("le tiers correspondant au conjoint n'a pas été trouvé", conjointDivorce);
		
		/*
		 * Il doit avoir un for principal ouvert et soumis au regime ordinaire
		 */
		assertNotNull("le for principal du conjoint n'a pas été ouvert", conjointDivorce.getForFiscalPrincipalAt(null));
		assertEquals("le conjoint devrait être soumis à l'impôt ordinaire", conjointDivorce.getForFiscalPrincipalAt(null).getModeImposition(),
				ModeImposition.ORDINAIRE);
		
		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(TIERS_MENAGE_COMMUN);
		assertNotNull("le tiers correspondant au menage commun n'a pas été trouvé", menageCommun);
		
		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final RapportEntreTiers rapportDivorces = habitantDivorce.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers divorcés n'a pas été clos", DATE_DIVORCE.getOneDayBefore(), rapportDivorces.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur chacun des conjoints
		 */
		assertEquals(3, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(habitantDivorce).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(conjointDivorce).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorcePersonneMarieeDeNationaliteInconnue() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne de nationalité inconnue.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, date(2008, 12, 31));
		Divorce divorce = createValidDivorce(marie, conjoint);
		
		final MessageCollector collector = buildMessageCollector();
		divorce.validate(collector, collector);
		assertTrue("l'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", collector.hasErreurs());
		
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDivorcePersonneSepare() throws EvenementCivilException {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne déjà séparée.");
		Individu separe = serviceCivil.getIndividu(INDIVIDU_SEPARE, date(2008, 12, 31));
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_SEPARE_CONJOINT, date(2008, 12, 31));
		Divorce divorce = createValidDivorce(separe, conjoint);
		
		final MessageCollector collector = buildMessageCollector();
		divorce.validate(collector, collector);
		assertTrue("l'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", collector.hasErreurs());
		
		divorce.handle(collector);

		/*
		 * Test de récupération du tiers qui divorce
		 */
		PersonnePhysique habitantDivorce = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE);
		assertNotNull("le tiers correspondant au divorcé n'a pas été trouvé", habitantDivorce);
		
		/*
		 * Test de récupération du conjoint
		 */
		PersonnePhysique conjointDivorce = tiersDAO.getHabitantByNumeroIndividu(INDIVIDU_MARIE_CONJOINT);
		assertNotNull("le tiers correspondant au conjoint n'a pas été trouvé", conjointDivorce);
		
		/*
		 * Aucun événement doit être généré car les individus sont déja séparés au moment du divorce
		 */
		assertEquals(0, eventSender.count);
		assertEquals(0, getEvenementFiscalService().getEvenementsFiscaux(habitantDivorce).size());
		assertEquals(0, getEvenementFiscalService().getEvenementsFiscaux(conjointDivorce).size());
	}

	private Divorce createValidDivorce(Individu individu, Individu conjoint) {
		return new Divorce(individu, conjoint, DATE_DIVORCE, 5652, context);
	}

	@Test
	public void testEvenementDivorceRedondant() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		// création d'un ménage-commun divorcé au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		// création d'un ménage-commun divorcé au fiscal
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(monsieur, dateDivorce.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				addForPrincipal(madame, dateDivorce.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Chamblon);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, dateDivorce);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						MockCommune.Echallens);
				return null;
			}
		});

		// traitement de l'événement de divorce redondant
		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Individu monsieur = serviceCivil.getIndividu(noMonsieur, dateDivorce);
				final Individu madame = serviceCivil.getIndividu(noMadame, dateDivorce);

				// la date de l'événement divorce corresponds au premier jour de non-appartenance ménage des composants (à l'inverse de la logique habituelle)
				final Divorce divorce = new Divorce(monsieur, madame, dateDivorce.getOneDayAfter(), MockCommune.Echallens.getNoOFSEtendu(), context);

				final MessageCollector collector = buildMessageCollector();
				divorce.validate(collector, collector);
				final HandleStatus etat = divorce.handle(collector);

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
				assertEquals(dateDivorce, appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateDivorce, appartenanceMadame.getDateFin());

				assertNull(tiersService.getEnsembleTiersCouple(madame, dateDivorce.getOneDayAfter()));
				return null;
			}
		});
	}
}
