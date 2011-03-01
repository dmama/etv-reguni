package ch.vd.uniregctb.evenement.divorce;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Test du handler de divorce:
 * ---------------------------
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(DivorceHandlerTest.class);

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
	private static final String DB_UNIT_DATA_FILE = "DivorceHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			
			@Override
			protected void init() {
				super.init();
				
				MockIndividu pierre = getIndividu(INDIVIDU_MARIE_SEUL);
				separeIndividu(pierre, null, DATE_SEPARATION);
				separeIndividu(pierre, null, DATE_DIVORCE);
				
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
	public void testDivorceCelibataire() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne seule.");
		Individu celibataire = serviceCivil.getIndividu(INDIVIDU_CELIBATAIRE, 2008);
		Divorce divorce = createValidDivorce(celibataire, null);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		divorce.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness du divorce.", erreurs);

		divorce.validate(erreurs, warnings);
		assertEquals("l'événement aurait du être en erreur car personne non marié", erreurs.isEmpty(), false);
		
	}
	
	@Test
	public void testDivorcePersonneMarieeSeule() {
	
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne mariée seule.");
		Individu marieSeul = serviceCivil.getIndividu(INDIVIDU_MARIE_SEUL, 2008);
		Divorce divorce = createValidDivorce(marieSeul, null);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		divorce.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness du divorce.", erreurs);
		
		divorce.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate du divorce.", erreurs);
		
		divorce.handle(warnings);
		
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
	public void testDivorcePersonneMarieeAvecSuisseOuPermisC() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne mariée avec un suisse ou étranger avec permis C.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE, 2008);
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE_CONJOINT, 2008);
		Divorce divorce = createValidDivorce(marie, conjoint);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		divorce.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness du divorce.", erreurs);
		
		divorce.validate(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du validate du divorce.", erreurs);
		
		divorce.handle(warnings);
		
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
	public void testDivorcePersonneMarieeDeNationaliteInconnue() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne de nationalité inconnue.");
		Individu marie = serviceCivil.getIndividu(INDIVIDU_MARIE2, 2008);
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_MARIE2_CONJOINT, 2008);
		Divorce divorce = createValidDivorce(marie, conjoint);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		divorce.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness du divorce.", erreurs);
		
		divorce.validate(erreurs, warnings);
		assertEquals("l'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", erreurs.isEmpty(), false);
		
	}
	
	@Test
	public void testDivorcePersonneSepare() {
		
		LOGGER.debug("Test de traitement d'un événement de divorce d'une personne déjà séparée.");
		Individu separe = serviceCivil.getIndividu(INDIVIDU_SEPARE, 2008);
		Individu conjoint = serviceCivil.getIndividu(INDIVIDU_SEPARE_CONJOINT, 2008);
		Divorce divorce = createValidDivorce(separe, conjoint);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		divorce.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness du divorce.", erreurs);
		
		divorce.validate(erreurs, warnings);
		assertEquals("l'événement aurait du être en erreur car impossible de déterminer la nationalité de la personne", erreurs.isEmpty(), false);
		
		divorce.handle(warnings);

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

		MockDivorce divorce = new MockDivorce();
		divorce.setIndividu(individu);
		divorce.setAncienConjoint(conjoint);
		
		divorce.setNumeroOfsCommuneAnnonce(5652);
		divorce.setDate(DATE_DIVORCE);

		divorce.setHandler(evenementCivilHandler);
		return divorce;
	
	}
	
}
