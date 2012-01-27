package ch.vd.uniregctb.evenement.civil.engine.regpp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.interne.testing.Testing;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPCriteria;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilCache;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class EvenementCivilProcessorTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilProcessorTest.class);

	private EvenementCivilProcessor evenementCivilProcessor;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private TiersDAO tiersDAO;
	private GlobalTiersSearcher searcher;
	private DefaultMockServiceCivil mockServiceCivil;

	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		mockServiceCivil = new DefaultMockServiceCivil();
		serviceCivil.setUp(mockServiceCivil);

		evenementCivilProcessor = getBean(EvenementCivilProcessor.class, "evenementCivilProcessor");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		evenementCivilRegPPDAO = getBean(EvenementCivilRegPPDAO.class, "evenementCivilRegPPDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testEvenementsSansType() throws Exception {

		saveEvenement(9000L, null, RegDate.get(2007, 10, 25), 983254L, null, 1010, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@Test
	public void testNumeroOfsInvalide() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 983254L, null, 12345678, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	/**
	 * @param tiers
	 */
	@Test
	public void testEvenementsSansIndividuPrincipal() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), null, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsNaissance() throws Exception {

		saveEvenement(9001L, TypeEvenementCivil.NAISSANCE, RegDate.get(2007, 10, 25), 983254L, null, 0, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.TRAITE, e);

				PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(983254L);
				assertNotNull(tiers);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsArriveeEnErreur() throws Exception {

		// Le CTB lié a cet individu n'existe pas
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, RegDate.get(2007, 10, 25), 34567L, null, 5402, EtatEvenementCivil.A_TRAITER);

		// Lancement du traitement des événements
		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsExceptionDansValidate() throws Exception {

		// L'evenement 123L throw une exception lors de la validation
		saveEvenement(123L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 78912L, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsErreursDansValidate() throws Exception {

		final long NO_INDIVIDU = 78912L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(NO_INDIVIDU, date(1944,12,12), "Rufus", "Bonpoil", true);
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique rufus = addHabitant(NO_INDIVIDU);
				assertNotNull(rufus);
				return null;
			}
		});

		saveEvenement(124L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), NO_INDIVIDU, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(e, "Check completeness erreur", "Again");

				return null;
			}
		});
	}

	@Test
	public void testEvenementsWarnDansValidate() throws Exception {

		final long noInd2 = 89123L;
		final long noInd1 = 78912L;
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique hab1 = new PersonnePhysique(true);
				hab1.setNumeroIndividu(noInd1);
				tiersDAO.save(hab1);

				final PersonnePhysique hab2 = new PersonnePhysique(true);
				hab2.setNumeroIndividu(noInd2);
				tiersDAO.save(hab2);
				return null;
			}
		});

		saveEvenement(125L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.A_VERIFIER, e);
				assertWarnings(1, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsPasDeTiersPrincipal() throws Exception {

		long noInd = 6789L;
		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(1, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementsPasDeTiersConjoint() throws Exception {

		final long noInd1 = 78912L;
		final long noInd2 = 89123L;
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumeroIndividu(noInd1);
				tiersDAO.save(hab);
				return null;
			}
		});

		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(1, e);

				return null;
			}
		});
	}

	@Test
	public void testEvenementExceptionDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoExceptionDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilRegPP e) {
				assertEquals(EtatEvenementCivil.EN_ERREUR, e.getEtat());

				final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.ERROR, erreur0.getType());
				assertEquals("Exception de test", erreur0.getMessage());
			}
		});
	}

	@Test
	public void testEvenementTraiteAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilRegPP e) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, e.getEtat());

				final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test
	public void testEvenementRedondantAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilRegPP e) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, e.getEtat());

				final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test
	public void testEvenementTraiteSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilRegPP e) {
				assertEquals(EtatEvenementCivil.TRAITE, e.getEtat());
				assertEmpty(e.getErreurs());
			}
		});
	}

	@Test
	public void testEvenementRedondantSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilRegPP e) {
				assertEquals(EtatEvenementCivil.REDONDANT, e.getEtat());
				assertEmpty(e.getErreurs());
			}
		});
	}

	private interface AfterHandleCallback {
		void checkEvent(EvenementCivilRegPP e);
	}

	private void traiterEvenementTesting(long noEventTesting, final AfterHandleCallback callback) throws Exception {

		final long noInd1 = 12345L;
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumeroIndividu(noInd1);
				tiersDAO.save(hab);
				return null;
			}
		});

		saveEvenement(noEventTesting, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());

				final EvenementCivilRegPP e = list.get(0);
				callback.checkEvent(e);
				return null;
			}
		});
	}

	/**
	 * Test l'intégration de l'événement départ dans le moteur d'événements
	 *
	 * @throws Exception
	 */
	@Test
	public void testEvenementsDepart() throws Exception {

		setUpHabitant();
		saveEvenement(9105L, TypeEvenementCivil.DEPART_COMMUNE, RegDate.get(2007, 10, 25), 34567L, null, MockCommune.Cossonay
				.getNoOFS(), EtatEvenementCivil.A_TRAITER);
		traiteEvenements();

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilRegPP e = list.get(0);
				//evenement en erreur car pas d'adresse de départ spécifiée
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				return null;
			}
		});

	}

	@Test
	public void testEvenementsChangementSexe() throws Exception {

		setWantIndexation(true);
		setUpHabitant();

		// Rech du tiers avant modif
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(67919191L);

		// changement du sexe dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(34567L);
		individu.setSexeMasculin(true);

		saveEvenement(9106L, TypeEvenementCivil.CHGT_SEXE, RegDate.get(2007, 10, 25), 34567L, null, MockCommune.Cossonay
				.getNoOFS(), EtatEvenementCivil.A_TRAITER);


		traiteEvenements();

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilRegPP> listEv = evenementCivilRegPPDAO.getAll();
				assertEquals(1, listEv.size());
				EvenementCivilRegPP e = listEv.get(0);
				assertEvtState(EtatEvenementCivil.TRAITE, e);

				// Nouvelle recherche
				List<TiersIndexedData> lTiers = searcher.search(criteria);
				Assert.isTrue(lTiers.size() == 1, "L'indexation n'a pas fonctionné");

				// on verifie que le changement a bien été effectué
				Individu indi = serviceCivil.getIndividu(34567L, RegDate.get());
				Assert.isTrue(indi.isSexeMasculin(), "le nouveau sexe n'a pas été indexé");

				// PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(983254L);
				// assertNotNull(tiers);

				return null;
			}
		});

		setWantIndexation(false);
	}

	/**
	 * [UNIREG-1200] Teste que le retraitement de plusieurs événements civil suite au traitement correct d'un événement fonctionne bien
	 */
	@Test
	public void testRetraitementPlusieursEvenementsCivil() throws Exception {

		final long noIndividu = 34567L; // Sophie Dupuis

		// Deux événements de changement de nom sur un individu sont déjà en erreur
		saveEvenement(9000L, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, RegDate.get(2007, 10, 24), noIndividu, null, MockCommune.Lausanne.getNoOFS(), EtatEvenementCivil.EN_ERREUR);
		saveEvenement(9001L, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, RegDate.get(2007, 10, 23), noIndividu, null, MockCommune.Lausanne.getNoOFS(), EtatEvenementCivil.EN_ERREUR);

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertNotNull(list);
				sortEvenements(list);
				assertEquals(2, list.size());
				assertEvenement(9000, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(0));
				assertEvenement(9001, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(1));
				return null;
			}
		});

		// Arrivée de l'événement d'arrivée -> ce dernier doit être traité et provoquer le retraitement des événements de changement de nom
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, RegDate.get(1980, 11, 2), noIndividu, null, MockCommune.Lausanne.getNoOFS(), EtatEvenementCivil.A_TRAITER);

		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
				assertNotNull(list);
				sortEvenements(list);
				assertEquals(3, list.size());
				assertEvenement(9000, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.TRAITE, list.get(0));
				assertEvenement(9001, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.TRAITE, list.get(1));
				assertEvenement(9002, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, EtatEvenementCivil.TRAITE, list.get(2));
				return null;
			}
		});
	}

	/**
	 * Trie la liste par ordre croissant d'id.
	 */
	private static void sortEvenements(final List<EvenementCivilRegPP> list) {
		Collections.sort(list, new Comparator<EvenementCivilRegPP>() {
			@Override
			public int compare(EvenementCivilRegPP o1, EvenementCivilRegPP o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
	}

	// ********************************************************************

	private void saveEvenement(final long id, final TypeEvenementCivil type, final RegDate date, final Long indPri, @Nullable final Long indSec, final int ofs, final EtatEvenementCivil etat) throws Exception {

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(id);
				evt.setType(type);
				evt.setDateEvenement(date);
				evt.setEtat(etat);
				evt.setNumeroIndividuPrincipal(indPri);
				evt.setNumeroIndividuConjoint(indSec);
				evt.setNumeroOfsCommuneAnnonce(ofs);
				evt = evenementCivilRegPPDAO.save(evt);
				if (etat == EtatEvenementCivil.EN_ERREUR) {
					evt.addErrors(Arrays.asList(new EvenementCivilRegPPErreur("Erreur de test")));
				}
				return evt;
			}
		});
	}

	private void assertErreurs(int expected, EvenementCivilRegPP e) {

		for (EvenementCivilRegPPErreur erreur : e.getErreurs()) {
			String msg = erreur.getMessage();
			LOGGER.debug(msg);
		}
		assertEquals(expected, e.getErreurs().size());
	}

	private void assertErreurs(EvenementCivilRegPP e, String... errors) {

		assertEquals(errors.length, e.getErreurs().size());

		final Set<String> expected = new HashSet<String>();
		for (EvenementCivilRegPPErreur ee : e.getErreurs()) {
			expected.add(ee.getMessage());
		}

		for (int i = 0, errorsLength = errors.length; i < errorsLength; i++) {
			final String error = errors[i];
			assertTrue("L'erreur [" + error + "] n'existe pas", expected.contains(error));
		}
	}

	private void assertWarnings(int expected, EvenementCivilRegPP e) {

		for (EvenementCivilRegPPErreur w : e.getWarnings()) {
			String msg = w.getMessage();
			LOGGER.debug(msg);
		}
		assertEquals(expected, e.getErreurs().size());
	}

	private void assertEvtState(EtatEvenementCivil expected, EvenementCivilRegPP e) {

		final boolean ok = e.getEtat() == expected;
		if (!ok) {
			// Dump les erreurs
			for (EvenementCivilRegPPErreur erreur : e.getErreurs()) {
				LOGGER.error(erreur.getMessage());
			}
			fail("L'evenement(" + e.getId() + ") devrait avoir l'etat " + expected + " alors qu'il a l'état " + e.getEtat());
		}
	}

	private static void assertEvenement(long id, TypeEvenementCivil type, EtatEvenementCivil etat, EvenementCivilRegPP e) {
		assertNotNull(e);
		assertEquals(id, e.getId().longValue());
		assertEquals(type, e.getType());
		assertEquals(etat, e.getEtat());
	}

	private void traiteEvenements() throws Exception {

		// Lancement du traitement des événements
		evenementCivilProcessor.traiteEvenementsCivils(null);
	}

	private void setUpHabitant() throws Exception {
		final long noIndividu = 34567; // Sophie Dupuis

		// Crée un habitant
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(noIndividu);
				habitant.setNumero(67919191L);
				addForPrincipal(habitant, RegDate.get(1980, 11, 2), null, MockCommune.Cossonay.getNoOFS());

				tiersDAO.save(habitant);
				return null;
			}
		});

		globalTiersIndexer.sync();
	}

	private ForFiscalPrincipal addForPrincipal(Tiers tiers, RegDate ouverture, RegDate fermeture, Integer noOFS) {
		final ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setDateFin(fermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.SOURCE);
		f.setMotifOuverture(MotifFor.ARRIVEE_HC);
		tiers.addForFiscal(f);
		return f;
	}

	/**
	 * Cas UNIREG-2785
	 */
	@Test
	public void testAncienHabitantMarieSansMenageRecevantCorrectionConjoint() throws Exception {

		final long noIndividuMonsieur = 12457319L;
		final long noIndividuMadame = 124573213L;
		final RegDate dateNaissanceMonsieur = date(1958, 12, 5);
		final RegDate dateNaissanceMadame = date(1959, 2, 8);
		final RegDate dateMariage = date(1983, 3, 12);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noIndividuMonsieur, dateNaissanceMonsieur, "Tartempion", "Marcel", true);
				final MockIndividu mme = addIndividu(noIndividuMadame, dateNaissanceMadame, "Tartempion", "Ursule", false);
				marieIndividus(mr, mme, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique mme = addHabitant(noIndividuMadame);
				addEnsembleTiersCouple(mme, null, dateMariage, null);

				final PersonnePhysique pp = addNonHabitant("Tartempion", "Marcel", dateNaissanceMonsieur, Sexe.MASCULIN);
				pp.setNumeroIndividu(noIndividuMonsieur);
				pp.setCategorieEtranger(CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L);

				final SituationFamille celibat = addSituation(pp, dateNaissanceMonsieur, dateMariage.getOneDayBefore(), 0);
				celibat.setEtatCivil(EtatCivil.CELIBATAIRE);

				final SituationFamille mariage = addSituation(pp, dateMariage, null, 0);
				mariage.setEtatCivil(EtatCivil.MARIE);

				// les fors
				final RegDate dateArrivee = date(2009, 1, 1);
				final RegDate dateDepart = date(2009, 8, 31);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.RoyaumeUni);

				return pp.getNumero();
			}
		});

		// création et traitement de l'événement civil
		final long evtId = 1234567890L;
		saveEvenement(evtId, TypeEvenementCivil.CORREC_CONJOINT, dateMariage, noIndividuMonsieur, null, MockCommune.Lausanne.getNoOFSEtendu(), EtatEvenementCivil.A_TRAITER);
		traiteEvenements();

		// vérification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final EvenementCivilRegPPCriteria criterion = new EvenementCivilRegPPCriteria();
				criterion.setNumeroIndividu(noIndividuMonsieur);
				final List<EvenementCivilRegPP> evts = evenementCivilRegPPDAO.find(criterion, null);
				assertNotNull(evts);
				assertEquals(1, evts.size());

				final EvenementCivilRegPP externe = evts.get(0);
				assertNotNull(externe);
				assertEquals(TypeEvenementCivil.CORREC_CONJOINT, externe.getType());
				assertEquals(EtatEvenementCivil.EN_ERREUR, externe.getEtat());
				assertEquals(1, externe.getErreurs().size());

				final EvenementCivilRegPPErreur erreur = externe.getErreurs().iterator().next();
				assertNotNull(erreur);

				final String msg = erreur.getMessage();
				final String[] lignes = msg.split("\n");
				assertEquals(3, lignes.length);
				assertTrue(lignes[0].endsWith("2 erreur(s) - 0 warning(s):"));
				assertTrue(lignes[1].endsWith(String.format("ne peut pas exister alors que le tiers [%d] appartient à un ménage-commun", ppId)));
				assertTrue(lignes[2].endsWith(String.format("ne peut pas exister alors que le tiers [%d] appartient à un ménage-commun", ppId)));
				return null;
			}
		});
	}

	/**
	 * [SIFISC-982] Vérifie que les exceptions levées dans les événements civils internes sont bien interceptées par le processor d'événements civils, et que les liens entre l'événement civil et les
	 * personnes physiques correspondantes sont bien préservés.
	 */
	@Test
	public void testExceptionDansEvenementCivilInterne() throws Exception {

		final long noIndividuMonsieur = 12457319L;
		final long evtId = 1234567890L;

		// on crée un individu avec un contribuable associé
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuMonsieur, date(1966, 3, 12), "Fussnacht", "Cyril", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividuMonsieur);
				return pp.getNumero();
			}
		});
		assertNotNull(ppId);

		// création et traitement d'un événement civil qui lève une exception (= suppression d'individu qui doit être traitée manuellement)
		doInNewTransactionAndSession(new TransactionCallback<EvenementCivilRegPP>() {
			@Override
			public EvenementCivilRegPP doInTransaction(TransactionStatus status) {
				final EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(evtId);
				evt.setType(TypeEvenementCivil.SUP_INDIVIDU);
				evt.setDateEvenement(date(2011, 1, 1));
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividuPrincipal(noIndividuMonsieur);
				evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFSEtendu());
				return evenementCivilRegPPDAO.save(evt);
			}
		});
		traiteEvenements();

		// on vérifie que :
		//  - l'événement est en erreur
		//  - que l'événement civil a été correctement associé avec son individu
		//  - que le message d'erreur est bien renseigné
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final EvenementCivilRegPPCriteria criterion = new EvenementCivilRegPPCriteria();
				criterion.setNumeroIndividu(noIndividuMonsieur);
				final List<EvenementCivilRegPP> evts = evenementCivilRegPPDAO.find(criterion, null);
				assertNotNull(evts);
				assertEquals(1, evts.size());

				final EvenementCivilRegPP externe = evts.get(0);
				assertNotNull(externe);
				assertEquals(TypeEvenementCivil.SUP_INDIVIDU, externe.getType());
				assertEquals(EtatEvenementCivil.EN_ERREUR, externe.getEtat());
				assertEquals(1, externe.getErreurs().size());

				final EvenementCivilRegPPErreur erreur = externe.getErreurs().iterator().next();
				assertNotNull(erreur);
				assertEquals("Veuillez effectuer cette opération manuellement", erreur.getMessage());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1607] lors d'un recyclage d'événement civil, ce serait bien de rafraîchir le cache des individus concernés
	 */
	@Test
	public void testRafraichissementCacheEvtCivilsSurRecyclage() throws Exception {

		final long noIndividu = 14563435356783512L;
		final long evtId = 12456234125L;

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final DataEventService dataEventService = getBean(DataEventService.class, "dataEventService");
		assertNotNull(dataEventService);

		final UniregCacheManager uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
		assertNotNull(uniregCacheManager);

		// Initialisation du service civil avec un cache
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		cache.setUniregCacheManager(uniregCacheManager);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
		cache.reset();
		try {
			serviceCivil.setUp(cache);

			// mise en place civile
			cache.setTarget(new DefaultMockServiceCivil() {
				@Override
				protected void init() {
					addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfredo", true);
				}
			});

			// mise en place fiscale, remplissage du cache du service civil sur l'individu
			final long ppid = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addHabitant(noIndividu);
					assertEquals("Alfredo Hitchcock", tiersService.getNomPrenom(pp));
					return pp.getNumero();
				}
			});

			// modification dans le service civil, mais pas de notification
			doModificationIndividu(noIndividu, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenom("Alfred");        // sans le "o"
				}
			});

			// création d'un événement en erreur
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilRegPP evt = new EvenementCivilRegPP();
					evt.setId(evtId);
					evt.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
					evt.setDateEvenement(date(2009, 1, 1));
					evt.setEtat(EtatEvenementCivil.EN_ERREUR);
					evt.setNumeroIndividuPrincipal(noIndividu);
					evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFSEtendu());
					evenementCivilRegPPDAO.save(evt);
					return null;
				}
			});

			// vérification que le nom contenu dans le cache du service civil est toujours celui qui est pris en compte
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
					final String prenomNom = tiersService.getNomPrenom(pp);
					assertEquals("Alfredo Hitchcock", prenomNom);
					return null;
				}
			});

			// demande le recyclage de l'événement en erreur
			evenementCivilProcessor.recycleEvenementCivil(evtId);

			// vérification que le cache du service civil a bien été rafraîchi
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
					final String prenomNom = tiersService.getNomPrenom(pp);
					assertEquals("Alfred Hitchcock", prenomNom);
					return null;
				}
			});
		}
		finally {
			serviceCivil.tearDown();
			cache.destroy();
		}
	}

	/**
	 * [SIFISC-2782] lors d'un forçage d'événement civil, ce serait bien de rafraîchir le cache des individus concernés
	 */
	@Test
	public void testRafraichissementCacheEvtCivilsSurForcage() throws Exception {

		final long noIndividu = 14563435356783512L;
		final long evtId = 12456234125L;

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final DataEventService dataEventService = getBean(DataEventService.class, "dataEventService");
		assertNotNull(dataEventService);

		final UniregCacheManager uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
		assertNotNull(uniregCacheManager);

		// Initialisation du service civil avec un cache
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		cache.setUniregCacheManager(uniregCacheManager);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
		cache.reset();
		try {
			serviceCivil.setUp(cache);

			// mise en place civile
			cache.setTarget(new DefaultMockServiceCivil() {
				@Override
				protected void init() {
					addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfredo", true);
				}
			});

			// mise en place fiscale, remplissage du cache du service civil sur l'individu
			final long ppid = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addHabitant(noIndividu);
					assertEquals("Alfredo Hitchcock", tiersService.getNomPrenom(pp));
					return pp.getNumero();
				}
			});

			// modification dans le service civil, mais pas de notification
			doModificationIndividu(noIndividu, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenom("Alfred");        // sans le "o"
				}
			});

			// création d'un événement en erreur
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilRegPP evt = new EvenementCivilRegPP();
					evt.setId(evtId);
					evt.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
					evt.setDateEvenement(date(2009, 1, 1));
					evt.setEtat(EtatEvenementCivil.EN_ERREUR);
					evt.setNumeroIndividuPrincipal(noIndividu);
					evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFSEtendu());
					evenementCivilRegPPDAO.save(evt);
					return null;
				}
			});

			// vérification que le nom contenu dans le cache du service civil est toujours celui qui est pris en compte
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
					final String prenomNom = tiersService.getNomPrenom(pp);
					assertEquals("Alfredo Hitchcock", prenomNom);
					return null;
				}
			});

			// forçage de l'événement en erreur
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(evtId);
					evenementCivilProcessor.forceEvenementCivil(evt);
					return null;
				}
			});

			// vérification que le cache du service civil a bien été rafraîchi
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
					final String prenomNom = tiersService.getNomPrenom(pp);
					assertEquals("Alfred Hitchcock", prenomNom);

					final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(evtId);
					assertNotNull(evt);
					assertEquals(EtatEvenementCivil.FORCE, evt.getEtat());

					return null;
				}
			});
		}
		finally {
			serviceCivil.tearDown();
			cache.destroy();
		}
	}
}
