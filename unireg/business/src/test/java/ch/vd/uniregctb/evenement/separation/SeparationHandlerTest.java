package ch.vd.uniregctb.evenement.separation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Test du handler de séparation:
 * ------------------------------
 * 
 * @author Pavel BLANCO
 *
 */
public class SeparationHandlerTest extends AbstractEvenementHandlerTest {
	
	private static final Logger LOGGER = Logger.getLogger(SeparationHandlerTest.class);
	
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
	private static final String DB_UNIT_DATA_FILE = "SeparationHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			
			@Override
			protected void init() {
				super.init();
				
				MockIndividu pierre = getIndividu(INDIVIDU_MARIE_SEUL);
				separeIndividu(pierre, null, DATE_SEPARATION);
				
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
	public void testSeparationPersonneMarieeSeule() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne mariée seule.");
		Individu marieSeul = serviceCivil.getIndividu(INDIVIDU_MARIE_SEUL, 2008);
		Separation separation = createValidSeparation(marieSeul, null);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);
		
		evenementCivilHandler.validate(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de la séparation.", erreurs);
		
		evenementCivilHandler.handle(separation, warnings);
		
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
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(habitantSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());

	}
	
	@Test
	public void testSeparationPersonneMarieeAvecSuisseOuPermisC() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne mariée avec un suisse ou étranger avec permis C.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE, 2008);
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE_CONJOINT, 2008);
		Separation separation = createValidSeparation(marie, conjoint);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);
		
		evenementCivilHandler.validate(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate de la séparation.", erreurs);
		
		evenementCivilHandler.handle(separation, warnings);
		
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
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(habitantSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(conjointSepare).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}
	
	@Test
	public void testSeparationPersonneMarieeDeNationaliteInconnue() throws Exception {
		
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de séparation d'une personne de nationalité inconnue.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, 2008);
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, 2008);
		Separation separation = createValidSeparation(marie, conjoint);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		evenementCivilHandler.checkCompleteness(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);
		
		evenementCivilHandler.validate(separation, erreurs, warnings);
		assertEquals("L'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", false, erreurs.isEmpty());
		
	}

	@Test
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
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique leon = addHabitant(INDIVIDU_MARIE2);
				final PersonnePhysique helene = addHabitant(INDIVIDU_MARIE2_CONJOINT);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(leon, helene, DATE_SEPARATION, null);        // on les marie au fiscal le jour où ils arrivent HC (ils vont en fait se séparer)
				addForPrincipal(ensemble.getMenage(), DATE_SEPARATION, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return null;
			}
		});

		final Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, 2008);
		final Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, 2008);
		final Separation separation = createValidSeparation(marie, conjoint);

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(separation, erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);

		evenementCivilHandler.validate(separation, erreurs, warnings);
		assertEquals("L'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", false, erreurs.isEmpty());

		try {
			evenementCivilHandler.handle(separation, warnings);
			Assert.fail("Le traitement de l'événement aurait dû lancer une exception");
		}
		catch (RuntimeException e) {
			final String message = e.getMessage();
			assertEquals("On ne peut fermer le rapport d'appartenance ménage avant sa date de début", message);
		}
	}
	
	private Separation createValidSeparation(Individu individu, Individu conjoint) {
		
		final MockSeparation separation = new MockSeparation();
		separation.setIndividu(individu);
		separation.setAncienConjoint(conjoint);
		separation.setNumeroOfsCommuneAnnonce(5652);
		separation.setDate(DATE_SEPARATION);
		return separation;
	}
}
