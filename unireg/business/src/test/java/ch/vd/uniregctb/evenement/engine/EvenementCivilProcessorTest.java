package ch.vd.uniregctb.evenement.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class EvenementCivilProcessorTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilProcessorTest.class);

	/**
	 * Une instance du moteur de règles.
	 */
	private EvenementCivilProcessor evenementCivilProcessor;

	/**
	 * La DAO pour les evenements
	 */
	private EvenementCivilDAO evenementCivilDAO;

	/**
	 * Le DAO.
	 */
	private TiersDAO tiersDAO;

	/**
	 * L'index global.
	 */
	private GlobalTiersSearcher searcher;

	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());

		evenementCivilProcessor = getBean(EvenementCivilProcessor.class, "evenementCivilProcessor");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		evenementCivilDAO = getBean(EvenementCivilDAO.class, "evenementCivilDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
	}

	/**
	 * @param tiers
	 */
	@NotTransactional
	@Test
	public void testEvenementsSansType() throws Exception {

		saveEvenement(9000L, null, RegDate.get(2007, 10, 25), 983254L, null, 1010);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testNumeroOfsInvalide() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 983254L, null, 12345678);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	/**
	 * @param tiers
	 */
	@NotTransactional
	@Test
	public void testEvenementsSansIndividuPrincipal() throws Exception {

		saveEvenement(9000L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), null, null, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsNaissance() throws Exception {

		saveEvenement(9001L, TypeEvenementCivil.NAISSANCE, RegDate.get(2007, 10, 25), 983254L, null, 0);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.TRAITE, e);

				PersonnePhysique tiers = tiersDAO.getHabitantByNumeroIndividu(983254L);
				assertNotNull(tiers);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsArriveeEnErreur() throws Exception {

		// Le CTB lié a cet individu n'existe pas
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, RegDate.get(2007, 10, 25), 34567L, null, 5402);

		// Lancement du traitement des événements
		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsExceptionDansCheckCompleteness() throws Exception {

		// L'evenement 123L throw une exception lors de la validation
		saveEvenement(123L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 78912L, null, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsErreursDansCheckCompleteness() throws Exception {

		saveEvenement(124L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), 78912L, null, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(2, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsWarnDansCheckCompleteness() throws Exception {

		final long noInd2 = 89123L;
		final long noInd1 = 78912L;
		doInNewTransaction(new TxCallback() {
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

		saveEvenement(125L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.A_VERIFIER, e);
				assertWarnings(1, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsIndividuConjointDifferentQueDansEvenement() throws Exception {

		final long noInd2 = 34567L;
		final long noInd1 = 78912L;
		doInNewTransaction(new TxCallback() {
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

		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(1, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsPasDeTiersPrincipal() throws Exception {

		long noInd = 6789L;
		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd, null, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(1, e);

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testEvenementsPasDeTiersConjoint() throws Exception {

		final long noInd1 = 78912L;
		final long noInd2 = 89123L;
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumeroIndividu(noInd1);
				tiersDAO.save(hab);
				return null;
			}
		});

		saveEvenement(120L, TypeEvenementCivil.EVENEMENT_TESTING, RegDate.get(2007, 10, 25), noInd1, noInd2, 5402);

		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				assertErreurs(1, e);

				return null;
			}
		});
	}

	/**
	 * Test l'integration de lévènement départ dans le moteur d'évenement
	 *
	 * @throws Exception
	 */
	@NotTransactional
	@Test
	public void testEvenementsDepart() throws Exception {

		setUpHabitant();
		saveEvenement(9105L, TypeEvenementCivil.DEPART_COMMUNE, RegDate.get(2007, 10, 25), 34567L, null, MockCommune.Cossonay
				.getNoOFS());
		traiteEvenements();

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertEquals(1, list.size());
				EvenementCivilData e = list.get(0);
				//evenement en erreur car pas d'adresse de départ spécifiée
				assertEvtState(EtatEvenementCivil.EN_ERREUR, e);
				return null;
			}
		});

	}

	@NotTransactional
	@Test
	public void testEvenementsChangementSexe() throws Exception {

		setWantIndexation(true);
		setUpHabitant();

		// Rech du tiers avant modif
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(67919191L);

		// changement du sexe dans le registre civil
		Individu individu = serviceCivil.getIndividu(34567L, RegDate.get().year());
		MockHistoriqueIndividu historiqueIndividu = (MockHistoriqueIndividu) individu.getDernierHistoriqueIndividu();
		historiqueIndividu.setSexe(Sexe.MASCULIN);

		saveEvenement(9106L, TypeEvenementCivil.CHGT_SEXE, RegDate.get(2007, 10, 25), 34567L, null, MockCommune.Cossonay
				.getNoOFS());


		traiteEvenements();

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Test de l'état des événements;
				List<EvenementCivilData> listEv = evenementCivilDAO.getAll();
				assertEquals(1, listEv.size());
				EvenementCivilData e = listEv.get(0);
				assertEvtState(EtatEvenementCivil.TRAITE, e);

				// Nouvelle recherche
				List<TiersIndexedData> lTiers = searcher.search(criteria);
				Assert.isTrue(lTiers.size() == 1, "L'indexation n'a pas fonctionné");
				Individu indi = serviceCivil.getIndividu(34567L, RegDate.get().year());
				MockHistoriqueIndividu histoIndi = (MockHistoriqueIndividu) indi.getDernierHistoriqueIndividu();

				// on verifie que le changement a bien été effectué
				Sexe sexeIndi = histoIndi.getSexe();
				Assert.isTrue(sexeIndi == Sexe.MASCULIN, "le nouveau sexe n'a pas été indexé");

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
	@NotTransactional
	@Test
	public void testRetraitementPlusieursEvenementsCivil() throws Exception {

		final long noIndividu = 34567L; // Sophie Dupuis

		// Deux événements de changement de nom sur un individu non arrivée -> ils doivent passer en erreur
		saveEvenement(9000L, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, RegDate.get(2007, 10, 24), noIndividu, null, MockCommune.Lausanne
				.getNoOFS());
		saveEvenement(9001L, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, RegDate.get(2007, 10, 23), noIndividu, null, MockCommune.Lausanne
				.getNoOFS());

		evenementCivilProcessor.traiteEvenementsCivils(null);

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilData> list = evenementCivilDAO.getAll();
				assertNotNull(list);
				sortEvenements(list);
				assertEquals(2, list.size());
				assertEvenement(9000, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(0));
				assertEvenement(9001, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, EtatEvenementCivil.EN_ERREUR, list.get(1));
				return null;
			}
		});

		// Arrivée de l'événement d'arrivée -> ce dernier doit être traité et provoquer le retraitement des événements de changement de nom
		saveEvenement(9002L, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, RegDate.get(1980, 11, 2), noIndividu, null, MockCommune.Lausanne
				.getNoOFS());

		evenementCivilProcessor.traiteEvenementCivil(9002L);

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilData> list = evenementCivilDAO.getAll();
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
	private static void sortEvenements(final List<EvenementCivilData> list) {
		Collections.sort(list, new Comparator<EvenementCivilData>() {
			public int compare(EvenementCivilData o1, EvenementCivilData o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
	}

	// ********************************************************************

	private void saveEvenement(final long id, final TypeEvenementCivil type, final RegDate date, final Long indPri, final Long indSec, final int ofs) throws Exception {

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				EvenementCivilData evt = new EvenementCivilData();
				evt.setId(id);
				evt.setType(type);
				evt.setDateEvenement(date);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividuPrincipal(indPri);
				evt.setNumeroIndividuConjoint(indSec);
				evt.setNumeroOfsCommuneAnnonce(ofs);
				evt = evenementCivilDAO.save(evt);
				return evt;
			}
		});
	}

	private void assertErreurs(int expected, EvenementCivilData e) {

		for (EvenementCivilErreur erreur : e.getErreurs()) {
			String msg = erreur.getMessage();
			LOGGER.debug(msg);
		}
		assertEquals(expected, e.getErreurs().size());
	}

	private void assertWarnings(int expected, EvenementCivilData e) {

		for (EvenementCivilErreur w : e.getWarnings()) {
			String msg = w.getMessage();
			LOGGER.debug(msg);
		}
		assertEquals(expected, e.getErreurs().size());
	}

	private void assertEvtState(EtatEvenementCivil expected, EvenementCivilData e) {

		final boolean ok = e.getEtat().equals(expected);
		if (!ok) {
			// Dump les erreurs
			for (EvenementCivilErreur erreur : e.getErreurs()) {
				LOGGER.error(erreur.getMessage());
			}
			fail("L'evenement(" + e.getId() + ") devrait avoir l'etat " + expected + " alors qu'il a l'état " + e.getEtat());
		}
	}

	private static void assertEvenement(long id, TypeEvenementCivil type, EtatEvenementCivil etat, EvenementCivilData e) {
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
		doInNewTransaction(new TxCallback() {
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
		ForFiscalPrincipal f = new ForFiscalPrincipal();
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
}
