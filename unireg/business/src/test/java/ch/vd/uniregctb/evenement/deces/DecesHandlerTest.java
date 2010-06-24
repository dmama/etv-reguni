package ch.vd.uniregctb.evenement.deces;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Tests unitaires du handler du décès.
 *
 * @author Ludovic BERTIN
 *
 */
public class DecesHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(DecesHandlerTest.class);

	/**
	 * Le numero d'individu du défunt célibataire.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_CELIBATAIRE = 6789L;

	/**
	 * Le numero d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE = 54321L;

	/**
	 * Le numero d'individu du veuf.
	 */
	private static final Long NO_INDIVIDU_VEUF = 23456L;

	/**
	 * Le numero de tiers du ménage commun.
	 */
	private static final Long NO_TIERS_MENAGE_COMMUN = 7004L;

	/**
	 * Le numero d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER = 78912L;

	/**
	 * Le numero d'individu du veuf.
	 */
	private static final Long NO_INDIVIDU_VEUF_ETRANGER = 89123L;

	/**
	 * Le numero de tiers du ménage commun.
	 */
	private static final Long NO_TIERS_MENAGE_COMMUN_ETRANGER = 7003L;

	/**
	 * La date de deces.
	 */
	private static final RegDate DATE_DECES = RegDate.get(2008, 1, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "DecesHandlerTest.xml";


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testDecesPersonneSeule() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne seule.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE, 2008);
		Deces deces = createValidDeces(celibataire, null);

		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(deces, erreurs, warnings);
		evenementCivilHandler.validate(deces, erreurs, warnings);
		evenementCivilHandler.handle(deces, warnings);

		assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);

		{
			PersonnePhysique defunt = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
			assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);
			assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));

			/*
			 * Evénements fiscaux devant être générés :
			 *  - fermeture for fiscal principal sur le défunt
			 */
			assertEquals(1, eventSender.count);
			assertEquals(1, getEvenementFiscalService().getEvenementFiscals(defunt).size());
		}

	}

	@Test
	public void testDecesPersonneMarieeAvecSuisseOuPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un suisse ou étranger avec permis C.");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE, 2008);
				Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF, 2008);
				Deces deces = createValidDeces(marie, conjoint);

				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				evenementCivilHandler.checkCompleteness(deces, erreurs, warnings);
				evenementCivilHandler.validate(deces, erreurs, warnings);
				evenementCivilHandler.handle(deces, warnings);

				assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);
				return null;
			}
		});

		/*
		 * Test de récupération du tiers defunt
		 */
		PersonnePhysique defunt = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE);
		assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);

		/*
		 * Ses for principaux actifs doivent avoir été fermés
		 */
		assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));

		/*
		 * une événement doit être créé et un événement doit être publié
		/*
		 * Test de récupération du tiers veuf
		 */
		PersonnePhysique veuf = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF);
		assertNotNull("le tiers correspondant au veuf n'a pas été trouvé", veuf);

		/*
		 * Il doit avoir un for principal ouvert et soumis au regime ordinaire
		 */
		assertNotNull("le for principal du veuf n'a pas été ouvert", veuf.getForFiscalPrincipalAt(null));
		assertEquals("le veuf devrait être soumis à l'impôt ordinaire", veuf.getForFiscalPrincipalAt(null).getModeImposition(),
				ModeImposition.ORDINAIRE);

		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_TIERS_MENAGE_COMMUN);
		assertNotNull("le tiers correspondant au menagecommun n'a pas été trouvé", menageCommun);

		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert et tous les rapport doivent être fermés
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final Set<RapportEntreTiers> rapports = menageCommun.getRapportsObjet();
		assertEquals(2, rapports.size());
		{
			final Iterator<RapportEntreTiers> iter = rapports.iterator();
			assertEquals(DATE_DECES, iter.next().getDateFin());
			assertEquals(DATE_DECES, iter.next().getDateFin());
		}
		final RapportEntreTiers rapportDefunt = defunt.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers defunt/menage n'a pas été clos", DATE_DECES, rapportDefunt.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le veuf
		 *  - création d'une nouvelle situation de famille sur le veuf
		 */
		assertEquals(3, eventSender.count);
		assertEquals(2, getEvenementFiscalService().getEvenementFiscals(veuf).size());
		assertEquals(0, getEvenementFiscalService().getEvenementFiscals(defunt).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	@Test
	public void testDecesPersonneMarieeAvecEtrangerSansPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un étranger sans permis C.");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER, 2008);
				Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_ETRANGER, 2008);
				Deces deces = createValidDeces(marie, conjoint);

				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				evenementCivilHandler.checkCompleteness(deces, erreurs, warnings);
				evenementCivilHandler.validate(deces, erreurs, warnings);
				evenementCivilHandler.handle(deces, warnings);

				assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);
				return null;
			}
		});

		/*
		 * Test de récupération du tiers defunt
		 */
		PersonnePhysique defunt = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER);
		assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);

		/*
		 * Ses for principaux actifs doivent avoir été fermés
		 */
		assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));

		/*
		 * Test de récupération du tiers menageCommun
		 */
		Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_TIERS_MENAGE_COMMUN_ETRANGER);
		assertNotNull("le tiers correspondant au menagecommun n'a pas été trouvé", menageCommun);

		/*
		 * Test sur le menage commun, il ne doit plus rester de for principal ouvert
		 */
		assertNull("le for principal du tiers MenageCommun n'a pas été fermé", menageCommun.getForFiscalPrincipalAt(null));
		final Set<RapportEntreTiers> rapports = menageCommun.getRapportsObjet();
		assertEquals(2, rapports.size());
		{
			final Iterator<RapportEntreTiers> iter = rapports.iterator();
			assertEquals(DATE_DECES, iter.next().getDateFin());
			assertEquals(DATE_DECES, iter.next().getDateFin());
		}
		final RapportEntreTiers rapportDefunt = defunt.getRapportsSujet().toArray(new RapportEntreTiers[0])[0];
		assertEquals("le rapport entre tiers defunt/menage n'a pas été clos", DATE_DECES, rapportDefunt.getDateFin());

		/*
		 * Test de récupération du tiers veuf
		 */
		PersonnePhysique veuf = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF_ETRANGER);
		assertNotNull("le tiers correspondant au veuf n'a pas été trouvé", veuf);

		/*
		 * son for principal doivent avoir été créé
		 */
		assertNotNull("le for principal du veuf n'a pas été créé", veuf.getForFiscalPrincipalAt(null));

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le veuf
		 *  - création d'une nouvelle situation de famille sur le veuf
		 */
		assertEquals(3, eventSender.count);
		assertEquals(2, getEvenementFiscalService().getEvenementFiscals(veuf).size());
		assertEquals(0, getEvenementFiscalService().getEvenementFiscals(defunt).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	private Deces createValidDeces(Individu individu, Individu conjoint) {

		MockDeces deces = new MockDeces();
		deces.setIndividu(individu);
		deces.setConjointSurvivant(conjoint);
		deces.setNumeroOfsCommuneAnnonce(5652);
		deces.setDate(DATE_DECES);
		deces.init(tiersDAO);

		return deces;
	}

}
