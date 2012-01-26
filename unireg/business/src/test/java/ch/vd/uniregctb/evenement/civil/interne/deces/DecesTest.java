package ch.vd.uniregctb.evenement.civil.interne.deces;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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
public class DecesTest extends AbstractEvenementCivilInterneTest {

	final static private RegDate DATE_DECES = RegDate.get(2008, 1, 1);

	/**
	 * Le numéro d'individu du défunt célibataire.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu du défunt marié seul.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE_SEUL = 12345L;

	/**
	 * Le numéro d'individu du défunt marié.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_MARIE = 54321L;

	/**
	 * Le numéro d'individu du veuf marié.
	 */
	private static final Long NO_INDIVIDU_VEUF_MARIE = 23456L;

	/**
	 * Le numéro d'individu du défunt pacsé.
	 */
	private static final Long NO_INDIVIDU_DEFUNT_PACSE = 45678L;

	/**
	 * Le numéro d'individu du veuf pacsé.
	 */
	private static final Long NO_INDIVIDU_VEUF_PACSE = 56789L;

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
	 * Teste la recuperation du conjoint survivant dans le cas d'un célibataire.
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetConjointSurvivantCelibataire() throws Exception {
		// Cas du célibataire
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
		EvenementCivilRegPP
				evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_CELIBATAIRE , 0L, 1234, null);
		Deces adapter = new Deces(evenement, contextSimple, options);
		Assert.isNull(adapter.getConjointSurvivant(), "le conjoint survivant d'un celibataire ne doit pas exister");
	}


	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un marié seul.
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetConjointSurvivantMarieSeul() throws Exception {
		// Cas du marié seul
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_MARIE_SEUL);
		EvenementCivilRegPP
				evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_MARIE_SEUL , 0L, 1234, null);
		Deces adapter = new Deces(evenement, contextSimple, options);
		Assert.isNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un marié seul ne doit pas exister");
	}

	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un marié en couple.
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetConjointSurvivantMarieCouple() throws Exception {
		// Cas du marié
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_MARIE);
		EvenementCivilRegPP evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_MARIE , 0L, 1234, null);
		Deces adapter = new Deces(evenement, contextSimple, options);
		Assert.notNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un marié doit exister");
		Assert.isTrue( adapter.getConjointSurvivant().getNoTechnique() == NO_INDIVIDU_VEUF_MARIE, "le conjoint survivant n'est pas celui attendu");
	}

	/**
	 * Teste la recuperation du conjoint survivant dans le cas d'un pacsé.
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetConjointSurvivantPacse() throws Exception {
		// Cas du pacsé
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_DEFUNT_PACSE);
		EvenementCivilRegPP evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.DECES, EtatEvenementCivil.A_TRAITER, DATE_DECES, NO_INDIVIDU_DEFUNT_PACSE , 0L, 1234, null);
		Deces adapter = new Deces(evenement, contextSimple, options);
		Assert.notNull( adapter.getConjointSurvivant(), "le conjoint survivant d'un pacsé doit pas exister");
		Assert.isTrue( adapter.getConjointSurvivant().getNoTechnique() == NO_INDIVIDU_VEUF_PACSE, "le conjoint survivant n'est pas celui attendu");
	}

	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new MockServiceInfrastructureService() {
		@Override
		protected void init() {
			// Pays
			pays.add(MockPays.Suisse);

			// Cantons
			cantons.add(MockCanton.Vaud);

			// Communes
			communesVaud.add(MockCommune.Lausanne);
			communesVaud.add(MockCommune.Cossonay);

			// Localités
			localites.add(MockLocalite.Lausanne);
			localites.add(MockLocalite.CossonayVille);
		}
	});

	// Crée les données du mock service civil
	ServiceCivilService serviceCivilSimple = new DefaultMockServiceCivil() {
		@Override
		protected void init() {
			MockIndividu momo = addIndividu(54321, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu pierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
			MockIndividu bea = addIndividu(23456, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
			MockIndividu julie = addIndividu(6789, RegDate.get(1977, 4, 19), "Goux", "Julie", false);
			MockIndividu david = addIndividu(45678, RegDate.get(1964, 1, 23), "Dagobert", "David", true);
			MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(pierre);
			addDefaultAdressesTo(bea);
			addDefaultAdressesTo(julie);
			addDefaultAdressesTo(david);
			addDefaultAdressesTo(julien);

			/* mariages, pacs */
			marieIndividus(momo, bea, RegDate.get(1986, 4, 8));
			marieIndividus(david, julien, RegDate.get(1986, 4, 8));
			marieIndividu(pierre, RegDate.get(1986, 4, 8));

			/* origines */
			addOrigine(bea, MockCommune.Lausanne);
			addOrigine(julien, MockCommune.Lausanne);
		}
	};

	private EvenementCivilContext contextSimple;

	private static final Logger LOGGER = Logger.getLogger(DecesTest.class);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "DecesTest.xml";


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil(false));
		contextSimple = new EvenementCivilContext(serviceCivilSimple, infrastructureService, tiersDAO);
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDecesPersonneSeule() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne seule.");

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		final Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_CELIBATAIRE, date(2008, 12, 31));
		final Deces deces = createValidDeces(celibataire, null, DATE_DECES);

		final MessageCollector collector = buildMessageCollector();
		deces.validate(collector, collector);
		deces.handle(collector);

		assertEmpty("Une erreur est survenue lors du traitement du deces", collector.getErreurs());

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
	@Transactional(rollbackFor = Throwable.class)
	public void testDecesPersonneMarieeAvecSuisseOuPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un suisse ou étranger avec permis C.");

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE, date(2008, 12, 31));
				final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF, date(2008, 12, 31));
				final Deces deces = createValidDeces(marie, conjoint, DATE_DECES);

				final MessageCollector collector = buildMessageCollector();
				deces.validate(collector, collector);
				deces.handle(collector);

				assertEmpty("Une erreur est survenue lors du traitement du deces", collector.getErreurs());
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
	@Transactional(rollbackFor = Throwable.class)
	public void testDecesPersonneMarieeAvecEtrangerSansPermisC() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de décès d'un personne mariée avec un étranger sans permis C.");

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique defunt = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER);
				assertTrue("le futur défunt n'est déjà plus habitant ?", defunt.isHabitantVD());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu marie = serviceCivil.getIndividu(NO_INDIVIDU_DEFUNT_MARIE_AVEC_ETRANGER, date(2008, 12, 31));
				final Individu conjoint = serviceCivil.getIndividu(NO_INDIVIDU_VEUF_ETRANGER, date(2008, 12, 31), AttributeIndividu.ADRESSES);
				final Deces deces = createValidDeces(marie, conjoint, DATE_DECES);

				final MessageCollector collector = buildMessageCollector();
				deces.validate(collector, collector);
				deces.handle(collector);

				assertEmpty("Une erreur est survenue lors du traitement du deces", collector.getErreurs());
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

	private Deces createValidDeces(Individu ppal, Individu conjoint, final RegDate dateDeces) {

		doModificationIndividu(ppal.getNoTechnique(), new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		return new Deces(ppal, conjoint, dateDeces, 5652, context);
	}

	/**
	 * Cas trouvé dans UNIREG-2653<p/>
	 * Un événement civil de décès doit faire passer le décédé en "non-habitant" même si le décès avait déjà été traité fiscalement
	 */
	@Test
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
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				pp.setDateDeces(dateDeces);
				return pp.getNumero();
			}
		});

		// vérification du flag "habitant"
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertEquals(dateDeces, pp.getDateDeces());
				assertTrue(pp.isHabitantVD());
				return null;
			}
		});

		// envoi de l'événement civil de décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu ind = serviceCivil.getIndividu(noIndividu, null);
				final Deces deces = createValidDeces(ind, null, dateDeces);

				final MessageCollector collector = buildMessageCollector();
				deces.validate(collector, collector);
				deces.handle(collector);

				assertEmpty("Une erreur est survenue lors du traitement du deces", collector.getErreurs());
				return null;
			}
		});

		// vérification du flag habitant sur le contribuable
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertEquals(dateDeces, pp.getDateDeces());
				assertFalse(pp.isHabitantVD());
				return null;
			}
		});
	}
}
