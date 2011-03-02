package ch.vd.uniregctb.evenement.civil.interne.deces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests unitaires du handler du décès.
 *
 * @author Ludovic BERTIN
 *
 */
@SuppressWarnings({"JavaDoc"})
public class DecesHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(DecesHandlerTest.class);

	/**
	 * Le numéro d'individu du défunt célibataire.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE = 54321L;

	/**
	 * Le numéro d'individu du veuf.
	 */
	private static final Long NO_INDIVIDU_VEUF = 23456L;

	/**
	 * Le numero de tiers du ménage commun.
	 */
	private static final Long NO_TIERS_MENAGE_COMMUN = 7004L;

	/**
	 * Le numéro d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER = 78912L;

	/**
	 * Le numéro d'individu du veuf.
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

		serviceCivil.setUp(new DefaultMockServiceCivil(false));
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testDecesPersonneSeule() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne seule.");

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		final Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE, 2008);
		final DecesAdapter deces = createValidDeces(celibataire, null, DATE_DECES);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		deces.checkCompleteness(erreurs, warnings);
		deces.validate(erreurs, warnings);
		deces.handle(warnings);

		assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);

		{
			final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
			assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);
			assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));
			assertFalse("le tiers décédé aurait dû passer non-habitant", defunt.isHabitantVD());

			/*
			 * Evénements fiscaux devant être générés :
			 *  - fermeture for fiscal principal sur le défunt
			 */
			assertEquals(1, eventSender.count);
			assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(defunt).size());
		}

	}

	@Test
	public void testDecesPersonneMarieeAvecSuisseOuPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un suisse ou étranger avec permis C.");

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE, 2008);
				final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF, 2008);
				final DecesAdapter deces = createValidDeces(marie, conjoint, DATE_DECES);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				deces.checkCompleteness(erreurs, warnings);
				deces.validate(erreurs, warnings);
				deces.handle(warnings);

				assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);
				return null;
			}
		});

		/*
		 * Test de récupération du tiers defunt
		 */
		final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE);
		assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);

		/*
		 * Il doit être passé non-habitant
		 */
		assertFalse("Le tiers défunt aurait dû devenir non-habitant", defunt.isHabitantVD());

		/*
		 * Ses for principaux actifs doivent avoir été fermés
		 */
		assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));

		/*
		 * une événement doit être créé et un événement doit être publié
		/*
		 * Test de récupération du tiers veuf
		 */
		final PersonnePhysique veuf = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF);
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
		final Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_TIERS_MENAGE_COMMUN);
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
		assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(veuf).size());
		assertEquals(0, getEvenementFiscalService().getEvenementsFiscaux(defunt).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	@Test
	public void testDecesPersonneMarieeAvecEtrangerSansPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un étranger sans permis C.");

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER, 2008);
				final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_ETRANGER, 2008, AttributeIndividu.ADRESSES);
				final DecesAdapter deces = createValidDeces(marie, conjoint, DATE_DECES);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				deces.checkCompleteness(erreurs, warnings);
				deces.validate(erreurs, warnings);
				deces.handle(warnings);

				assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);
				return null;
			}
		});

		/*
		 * Test de récupération du tiers defunt
		 */
		final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER);
		assertNotNull("le tiers correspondant au défunt n'a pas été trouvé", defunt);

		/*
		 * Il doit être passé non-habitant
		 */
		assertFalse("Le tiers défunt aurait dû devenir non-habitant", defunt.isHabitantVD());

		/*
		 * Ses for principaux actifs doivent avoir été fermés
		 */
		assertNull("le for principal du defunt n'a pas été fermé", defunt.getForFiscalPrincipalAt(null));

		/*
		 * Test de récupération du tiers menageCommun
		 */
		final Contribuable menageCommun = tiersDAO.getContribuableByNumero(NO_TIERS_MENAGE_COMMUN_ETRANGER);
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
		final PersonnePhysique veuf = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_VEUF_ETRANGER);
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
		assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(veuf).size());
		assertEquals(0, getEvenementFiscalService().getEvenementsFiscaux(defunt).size());
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	private DecesAdapter createValidDeces(Individu ppal, Individu conjoint, final RegDate dateDeces) {

		doModificationIndividu(ppal.getNoTechnique(), new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(ppal.getNoTechnique(), true);
		final Long conjointPPId = (conjoint == null ? null : tiersDAO.getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), true));

		return new DecesAdapter(ppal, principalPPId, conjoint, conjointPPId, dateDeces, 5652, context);
	}

	/**
	 * Cas trouvé dans UNIREG-2653<p/>
	 * Un événement civil de décès doit faire passer le décédé en "non-habitant" même si le décès avait déjà été traité fiscalement
	 */
	@Test
	@NotTransactional
	public void testDecesCivilApresDecesFiscalEtFlagHabitant() throws Exception {

		final long noIndividu = 12356723L;
		final RegDate dateDeces = date(2009, 5, 12);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1934, 2, 4), "Riddle", "Tom", true);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				pp.setDateDeces(dateDeces);
				return pp.getNumero();
			}
		});

		// vérification du flag "habitant"
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertEquals(dateDeces, pp.getDateDeces());
				assertTrue(pp.isHabitantVD());
				return null;
			}
		});

		// envoi de l'événement civil de décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final Individu ind = serviceCivil.getIndividu(noIndividu, 2400);
				final DecesAdapter deces = createValidDeces(ind, null, dateDeces);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				deces.checkCompleteness(erreurs, warnings);
				deces.validate(erreurs, warnings);
				deces.handle(warnings);

				assertEmpty("Une erreur est survenue lors du traitement du deces", erreurs);
				return null;
			}
		});

		// vérification du flag habitant sur le contribuable
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertEquals(dateDeces, pp.getDateDeces());
				assertFalse(pp.isHabitantVD());
				return null;
			}
		});
	}
}
