package ch.vd.unireg.evenement.civil.engine.regpp;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.data.CivilDataEventNotifierImpl;
import ch.vd.unireg.data.PluggableCivilDataEventNotifier;
import ch.vd.unireg.evenement.civil.EvenementCivilCriteria;
import ch.vd.unireg.evenement.civil.interne.testing.Testing;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.cache.IndividuConnectorCache;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeEvenementErreur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class EvenementCivilProcessorTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilProcessorTest.class);

	private EvenementCivilProcessor evenementCivilProcessor;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private TiersDAO tiersDAO;
	private GlobalTiersSearcher searcher;
	private DefaultMockIndividuConnector mockServiceCivil;
	private PluggableCivilDataEventNotifier pluggableCivilDataEventNotifier;

	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		mockServiceCivil = new DefaultMockIndividuConnector();
		serviceCivil.setUp(mockServiceCivil);

		evenementCivilProcessor = getBean(EvenementCivilProcessor.class, "evenementCivilProcessor");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		evenementCivilRegPPDAO = getBean(EvenementCivilRegPPDAO.class, "evenementCivilRegPPDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		pluggableCivilDataEventNotifier = getBean(PluggableCivilDataEventNotifier.class, "civilDataEventNotifier");
	}

	@Override
	public void onTearDown() throws Exception {
		this.pluggableCivilDataEventNotifier.setTarget(null);
		super.onTearDown();
	}

	@Test
	public void testEvenementsSansType() throws Exception {

		saveEvenement(9000L, null, RegDate.get(2007, 10, 25), 983254L, null, 1010, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});
	}

	@Test
	public void testNumeroOfsInvalide() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 983254L, null, 12345678, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});
	}

	@Test
	public void testEvenementsSansIndividuPrincipal() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), null, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});
	}

	@Test
	public void testEvenementsNaissance() throws Exception {

		saveEvenement(9001L, TypeEvenementCivil.NAISSANCE, RegDate.get(2007, 10, 25), 983254L, null, 0, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.TRAITE, e);

			PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(983254L);
			assertNotNull(tiers);
			return null;
		});
	}

	@Test
	public void testEvenementsArriveeEnErreur() throws Exception {

		// Le CTB lié a cet individu n'existe pas
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, RegDate.get(2007, 10, 25), 34567L, null, 5402, EtatEvenementCivil.A_TRAITER);

		// Lancement du traitement des événements
		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});
	}

	@Test
	public void testEvenementsExceptionDansValidate() throws Exception {

		// L'evenement 123L throw une exception lors de la validation
		saveEvenement(123L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 78912L, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});
	}

	@Test
	public void testEvenementsErreursDansValidate() throws Exception {

		final long NO_INDIVIDU = 78912L;

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(NO_INDIVIDU, date(1944,12,12), "Rufus", "Bonpoil", true);
			}
		});

		doInNewTransaction(status -> {
			PersonnePhysique rufus = addHabitant(NO_INDIVIDU);
			assertNotNull(rufus);
			return null;
		});

		saveEvenement(124L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), NO_INDIVIDU, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			assertErreurs(e, "Check completeness erreur", "Again");
			return null;
		});
	}

	@Test
	public void testEvenementsWarnDansValidate() throws Exception {

		final long noInd2 = 89123L;
		final long noInd1 = 78912L;
		doInNewTransaction(status -> {
			final PersonnePhysique hab1 = new PersonnePhysique(true);
			hab1.setNumeroIndividu(noInd1);
			tiersDAO.save(hab1);

			final PersonnePhysique hab2 = new PersonnePhysique(true);
			hab2.setNumeroIndividu(noInd2);
			tiersDAO.save(hab2);
			return null;
		});

		saveEvenement(125L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.A_VERIFIER, e);
			assertWarnings(1, e);
			return null;
		});
	}

	@Test
	public void testEvenementsPasDeTiersPrincipal() throws Exception {

		long noInd = 6789L;
		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			assertErreurs(1, e);
			return null;
		});
	}

	@Test
	public void testEvenementsPasDeTiersConjoint() throws Exception {

		final long noInd1 = 78912L;
		final long noInd2 = 89123L;
		doInNewTransaction(status -> {
			final PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumeroIndividu(noInd1);
			tiersDAO.save(hab);
			return null;
		});

		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			assertErreurs(1, e);
			return null;
		});
	}

	@Test
	public void testEvenementExceptionDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoExceptionDansHandle, e -> {
			assertEquals(EtatEvenementCivil.EN_ERREUR, e.getEtat());

			final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
			assertNotNull(erreur0);
			assertEquals(TypeEvenementErreur.ERROR, erreur0.getType());
			assertEquals("Exception de test", erreur0.getMessage());
		});
	}

	@Test
	public void testEvenementTraiteAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteAvecWarningDansHandle, e -> {
			assertEquals(EtatEvenementCivil.A_VERIFIER, e.getEtat());

			final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
			assertNotNull(erreur0);
			assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
			assertEquals("Warning de test", erreur0.getMessage());
		});
	}

	@Test
	public void testEvenementRedondantAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantAvecWarningDansHandle, e -> {
			assertEquals(EtatEvenementCivil.A_VERIFIER, e.getEtat());

			final Set<EvenementCivilRegPPErreur> erreurs = e.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final EvenementCivilRegPPErreur erreur0 = erreurs.iterator().next();
			assertNotNull(erreur0);
			assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
			assertEquals("Warning de test", erreur0.getMessage());
		});
	}

	@Test
	public void testEvenementTraiteSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteSansWarning, e -> {
			assertEquals(EtatEvenementCivil.TRAITE, e.getEtat());
			assertEmpty(e.getErreurs());
		});
	}

	@Test
	public void testEvenementRedondantSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantSansWarning, e -> {
			assertEquals(EtatEvenementCivil.REDONDANT, e.getEtat());
			assertEmpty(e.getErreurs());
		});
	}

	private interface AfterHandleCallback {
		void checkEvent(EvenementCivilRegPP e);
	}

	private void traiterEvenementTesting(long noEventTesting, final AfterHandleCallback callback) throws Exception {

		final long noInd1 = 12345L;
		doInNewTransaction(status -> {
			final PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumeroIndividu(noInd1);
			tiersDAO.save(hab);
			return null;
		});

		saveEvenement(noEventTesting, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, null, 5402, EtatEvenementCivil.A_TRAITER);

		traiteEvenements();

		doInNewTransaction(status -> {
			final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());

			final EvenementCivilRegPP e = list.get(0);
			callback.checkEvent(e);
			return null;
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

		doInNewReadOnlyTransaction(status -> {
			List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertEquals(1, list.size());
			EvenementCivilRegPP e = list.get(0);
			//evenement en erreur car pas d'adresse de départ spécifiée
			assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
			return null;
		});

	}

	@Test
	public void testEvenementsChangementSexe() throws Exception {

		setWantIndexationTiers(true);
		setUpHabitant();

		// Rech du tiers avant modif
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(67919191L);

		// changement du sexe dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(34567L);
		individu.setSexe(Sexe.MASCULIN);

		saveEvenement(9106L, TypeEvenementCivil.CHGT_SEXE, RegDate.get(2007, 10, 25), 34567L, null, MockCommune.Cossonay
				.getNoOFS(), EtatEvenementCivil.A_TRAITER);


		traiteEvenements();

		// indexation en cours...
		globalTiersIndexer.sync();

		doInNewTransaction(status -> {
			// Test de l'état des événements;
			List<EvenementCivilRegPP> listEv = evenementCivilRegPPDAO.getAll();
			assertEquals(1, listEv.size());
			EvenementCivilRegPP e = listEv.get(0);
			assertEvtState(EtatEvenementCivil.TRAITE, e);

			// Nouvelle recherche
			List<TiersIndexedData> lTiers = searcher.search(criteria);
			assertEquals("L'indexation n'a pas fonctionné", 1, lTiers.size());

			// on verifie que le changement a bien été effectué
			Individu indi = serviceCivil.getIndividu(34567L, RegDate.get());
			assertEquals("le nouveau sexe n'a pas été indexé", Sexe.MASCULIN, indi.getSexe());

			// PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(983254L);
			// assertNotNull(tiers)

			return null;
		});

		setWantIndexationTiers(false);
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

		doInNewTransaction(status -> {
			final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertNotNull(list);
			sortEvenements(list);
			assertEquals(2, list.size());
			assertEvenement(9000, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(0));
			assertEvenement(9001, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(1));
			return null;
		});

		// Arrivée de l'événement d'arrivée -> ce dernier doit être traité et provoquer le retraitement des événements de changement de nom
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, RegDate.get(1980, 11, 2), noIndividu, null, MockCommune.Lausanne.getNoOFS(), EtatEvenementCivil.A_TRAITER);

		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInNewTransaction(status -> {
			final List<EvenementCivilRegPP> list = evenementCivilRegPPDAO.getAll();
			assertNotNull(list);
			sortEvenements(list);
			assertEquals(3, list.size());
			assertEvenement(9000, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.TRAITE, list.get(0));
			assertEvenement(9001, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.TRAITE, list.get(1));
			assertEvenement(9002, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, EtatEvenementCivil.TRAITE, list.get(2));
			return null;
		});
	}

	/**
	 * Trie la liste par ordre croissant d'id.
	 */
	private static void sortEvenements(final List<EvenementCivilRegPP> list) {
		Collections.sort(list, (o1, o2) -> o1.getId().compareTo(o2.getId()));
	}

	// ********************************************************************

	private void saveEvenement(final long id, final TypeEvenementCivil type, final RegDate date, final Long indPri, @Nullable final Long indSec, final int ofs, final EtatEvenementCivil etat) throws Exception {

		doInNewTransaction(status -> {
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
				evt.addErrors(Collections.singletonList(new EvenementCivilRegPPErreur("Erreur de test")));
			}
			return evt;
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

		final Set<String> expected = new HashSet<>();
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
		doInNewTransaction(status -> {
			final PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);
			habitant.setNumero(67919191L);
			addForPrincipal(habitant, RegDate.get(1980, 11, 2), null, MockCommune.Cossonay.getNoOFS());

			tiersDAO.save(habitant);
			return null;
		});

		globalTiersIndexer.sync();
	}

	private ForFiscalPrincipal addForPrincipal(Tiers tiers, RegDate ouverture, RegDate fermeture, Integer noOFS) {
		final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noIndividuMonsieur, dateNaissanceMonsieur, "Tartempion", "Marcel", true);
				final MockIndividu mme = addIndividu(noIndividuMadame, dateNaissanceMadame, "Tartempion", "Ursule", false);
				marieIndividus(mr, mme, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
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
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.RoyaumeUni);
			return pp.getNumero();
		});

		// création et traitement de l'événement civil
		final long evtId = 1234567890L;
		saveEvenement(evtId, TypeEvenementCivil.CORREC_CONJOINT, dateMariage, noIndividuMonsieur, null, MockCommune.Lausanne.getNoOFS(), EtatEvenementCivil.A_TRAITER);
		traiteEvenements();

		// vérification
		doInNewTransactionAndSession(status -> {
			final EvenementCivilCriteria criterion = new EvenementCivilCriteria();
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
			assertTrue(lignes[0].endsWith("2 erreur(s) - 0 avertissement(s):"));
			assertTrue(lignes[1].endsWith(String.format("ne peut pas exister alors que le tiers [%d] appartient à un ménage-commun", ppId)));
			assertTrue(lignes[2].endsWith(String.format("ne peut pas exister alors que le tiers [%d] appartient à un ménage-commun", ppId)));
			return null;
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividuMonsieur, date(1966, 3, 12), "Fussnacht", "Cyril", true);
			}
		});

		final Long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividuMonsieur);
			return pp.getNumero();
		});
		assertNotNull(ppId);

		// création et traitement d'un événement civil qui lève une exception (= suppression d'individu qui doit être traitée manuellement)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilRegPP evt = new EvenementCivilRegPP();
			evt.setId(evtId);
			evt.setType(TypeEvenementCivil.SUP_INDIVIDU);
			evt.setDateEvenement(date(2011, 1, 1));
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividuPrincipal(noIndividuMonsieur);
			evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());
			return evenementCivilRegPPDAO.save(evt);
		});
		traiteEvenements();

		// on vérifie que :
		//  - l'événement est en erreur
		//  - que l'événement civil a été correctement associé avec son individu
		//  - que le message d'erreur est bien renseigné
		doInNewTransactionAndSession(status -> {
			final EvenementCivilCriteria criterion = new EvenementCivilCriteria();
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

		final CivilDataEventNotifier civilDataEventNotifier = getBean(CivilDataEventNotifier.class, "civilDataEventNotifier");
		assertNotNull(civilDataEventNotifier);

		// Initialisation du service civil avec un cache
		final IndividuConnectorCache cache = new IndividuConnectorCache();
		cache.setCache(cacheManager.getCache("serviceCivil"));
		cache.afterPropertiesSet();
		cache.reset();
		pluggableCivilDataEventNotifier.setTarget(new CivilDataEventNotifierImpl(Collections.singletonList(cache)));

		try {
			serviceCivil.setUp(cache);

			// mise en place civile
			cache.setTarget(new DefaultMockIndividuConnector() {
				@Override
				protected void init() {
					addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfredo", true);
				}
			});

			// mise en place fiscale, remplissage du cache du service civil sur l'individu
			final long ppid = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				assertEquals("Alfredo Hitchcock", tiersService.getNomPrenom(pp));
				return pp.getNumero();
			});

			// modification dans le service civil, mais pas de notification
			doModificationIndividu(noIndividu, individu -> {
				individu.setPrenomUsuel("Alfred");        // sans le "o"
			});

			// création d'un événement en erreur
			doInNewTransactionAndSession(status -> {
				final EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(evtId);
				evt.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				evt.setDateEvenement(date(2009, 1, 1));
				evt.setEtat(EtatEvenementCivil.EN_ERREUR);
				evt.setNumeroIndividuPrincipal(noIndividu);
				evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());
				evenementCivilRegPPDAO.save(evt);
				return null;
			});

			// vérification que le nom contenu dans le cache du service civil est toujours celui qui est pris en compte
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfredo Hitchcock", prenomNom);
				return null;
			});

			// demande le recyclage de l'événement en erreur
			evenementCivilProcessor.recycleEvenementCivil(evtId);

			// vérification que le cache du service civil a bien été rafraîchi
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfred Hitchcock", prenomNom);
				return null;
			});
		}
		finally {
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

		final CivilDataEventNotifier civilDataEventNotifier = getBean(CivilDataEventNotifier.class, "civilDataEventNotifier");
		assertNotNull(civilDataEventNotifier);

		// Initialisation du service civil avec un cache
		final IndividuConnectorCache cache = new IndividuConnectorCache();
		cache.setCache(cacheManager.getCache("serviceCivil"));
		cache.afterPropertiesSet();
		cache.reset();
		pluggableCivilDataEventNotifier.setTarget(new CivilDataEventNotifierImpl(Collections.singletonList(cache)));

		try {
			serviceCivil.setUp(cache);

			// mise en place civile
			cache.setTarget(new DefaultMockIndividuConnector() {
				@Override
				protected void init() {
					addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfredo", true);
				}
			});

			// mise en place fiscale, remplissage du cache du service civil sur l'individu
			final long ppid = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				assertEquals("Alfredo Hitchcock", tiersService.getNomPrenom(pp));
				return pp.getNumero();
			});

			// modification dans le service civil, mais pas de notification
			doModificationIndividu(noIndividu, individu -> {
				individu.setPrenomUsuel("Alfred");        // sans le "o"
			});

			// création d'un événement en erreur
			doInNewTransactionAndSession(status -> {
				final EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(evtId);
				evt.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				evt.setDateEvenement(date(2009, 1, 1));
				evt.setEtat(EtatEvenementCivil.EN_ERREUR);
				evt.setNumeroIndividuPrincipal(noIndividu);
				evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());
				evenementCivilRegPPDAO.save(evt);
				return null;
			});

			// vérification que le nom contenu dans le cache du service civil est toujours celui qui est pris en compte
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfredo Hitchcock", prenomNom);
				assertTrue(pp.isHabitantVD());
				return null;
			});

			// forçage de l'événement en erreur
			doInNewTransactionAndSession(status -> {
				final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(evtId);
				evenementCivilProcessor.forceEvenementCivil(evt);
				return null;
			});

			// vérification que le cache du service civil a bien été rafraîchi
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfred Hitchcock", prenomNom);
				assertFalse(pp.isHabitantVD()); // [SIFISC-6908] le forçage de l'événement doit recalculer le flag 'habitant'

				final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.FORCE, evt.getEtat());
				return null;
			});
		}
		finally {
			cache.destroy();
		}
	}

	/**
	 * [SIFISC-5806] lors du traitement du batch de relance des événements civils, ce serait bien de rafraîchir le cache des individus concernés
	 */
	@Test
	public void testRafraichissementCacheEvtCivilsDansBatchDeRelance() throws Exception {

		final long noIndividu = 14563435356783512L;
		final long evtId = 12456234125L;

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final CivilDataEventNotifier civilDataEventNotifier = getBean(CivilDataEventNotifier.class, "civilDataEventNotifier");
		assertNotNull(civilDataEventNotifier);

		// Initialisation du service civil avec un cache
		final IndividuConnectorCache cache = new IndividuConnectorCache();
		cache.setCache(cacheManager.getCache("serviceCivil"));
		cache.afterPropertiesSet();
		cache.reset();
		pluggableCivilDataEventNotifier.setTarget(new CivilDataEventNotifierImpl(Collections.singletonList(cache)));

		try {
			serviceCivil.setUp(cache);

			// mise en place civile
			cache.setTarget(new DefaultMockIndividuConnector() {
				@Override
				protected void init() {
					addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfredo", true);
				}
			});

			// mise en place fiscale, remplissage du cache du service civil sur l'individu
			final long ppid = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				assertEquals("Alfredo Hitchcock", tiersService.getNomPrenom(pp));
				return pp.getNumero();
			});

			// modification dans le service civil, mais pas de notification
			doModificationIndividu(noIndividu, individu -> {
				individu.setPrenomUsuel("Alfred");        // sans le "o"
			});

			// création d'un événement en erreur
			doInNewTransactionAndSession(status -> {
				final EvenementCivilRegPP evt = new EvenementCivilRegPP();
				evt.setId(evtId);
				evt.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				evt.setDateEvenement(date(2009, 1, 1));
				evt.setEtat(EtatEvenementCivil.EN_ERREUR);
				evt.setNumeroIndividuPrincipal(noIndividu);
				evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFS());
				evenementCivilRegPPDAO.save(evt);
				return null;
			});

			// vérification que le nom contenu dans le cache du service civil est toujours celui qui est pris en compte
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfredo Hitchcock", prenomNom);
				return null;
			});

			traiteEvenements();

			// vérification que le cache du service civil a bien été rafraîchi
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final String prenomNom = tiersService.getNomPrenom(pp);
				assertEquals("Alfred Hitchcock", prenomNom);

				final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			});
		}
		finally {
			cache.destroy();
		}
	}
}
