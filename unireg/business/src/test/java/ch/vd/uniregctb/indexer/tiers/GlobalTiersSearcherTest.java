package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.Fuse;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Classe pour tester la recherche de tiers
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 */
@SuppressWarnings({"JavaDoc"})
public class GlobalTiersSearcherTest extends BusinessTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTiersSearcherTest.class);

	private static final String DB_UNIT_DATA_FILE = "GlobalTiersSearcherTest.xml";

	public GlobalTiersSearcherTest() {
		setWantIndexationTiers(true);
	}

	private static int getDefaultCollectivitesAdministrativesNumber() {
		return MockCollectiviteAdministrative.getAll().size();
	}

	/**
	 * @see ch.vd.uniregctb.common.AbstractBusinessTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		servicePM.setUp(new DefaultMockServicePM());

		serviceCivil.setUp(new MockServiceCivil() {
			/**
			 * Indexe des tiers
			 */
			@Override
			protected void init() {
				MockIndividu alain = addIndividu(9876, RegDate.get(1976, 2, 27), "Despont", "Alain", true);
				MockIndividu richard = addIndividu(9734, RegDate.get(1942, 12, 7), "Bolomey", "Richard", true);
				MockIndividu james = addIndividu(1373, RegDate.get(1992, 1, 14), "Dean", "James", true);
				MockIndividu francois = addIndividu(403399, RegDate.get(1961, 3, 12), "Lestourgie", "Francois", true);
				MockIndividu claudine = addIndividu(222, RegDate.get(1975, 11, 30), "Desplatanes", "Claudine", false);
				MockIndividu alain2 = addIndividu(111, RegDate.get(1965, 5, 21), "Despont", "Alain", true);
				MockIndividu miro = addIndividu(333, RegDate.get(1972, 7, 15), "Boillat dupain", "Miro", true);
				MockIndividu claudine2 = addIndividu(444, RegDate.get(1922, 2, 12), "Desplatanes", "Claudine", false);

				addFieldsIndividu(richard, "1234567891023", "98765432109", null);

				addDefaultAdressesTo(alain);
				addDefaultAdressesTo(richard);
				addDefaultAdressesTo(james);
				addDefaultAdressesTo(francois);
				addDefaultAdressesTo(claudine);
				addDefaultAdressesTo(alain2);
				addDefaultAdressesTo(miro);
				addDefaultAdressesTo(claudine2);
			}

			@SuppressWarnings("deprecation")
			private void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, null, null, MockLocalite.Bex.getNPA(), MockLocalite.Bex, new CasePostale(TexteCasePostale.CASE_POSTALE, 4848),
				           RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, null, null, MockLocalite.Renens.getNPA(), MockLocalite.Renens, new CasePostale(TexteCasePostale.CASE_POSTALE, 5252),
				           RegDate.get(1980, 11, 2), null);
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParNumeroContribuable() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(5434L);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(5434L);
			// Donc le nom n'est pas utilisé
			criteria.setNomRaison("Bla bla");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(5434L);
			// Donc le type de tiers n'est pas utilisé
			criteria.setTypeTiers(TypeTiers.AUTRE_COMMUNAUTE);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(5434L);
			// mais le type de tiers impératif est utilisé
			criteria.setTypeTiersImperatif(TypeTiers.AUTRE_COMMUNAUTE);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(1234456L); // Inexistant
			criteria.setNomRaison("Bolomido"); // Bolomido n'est pas utilisé
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSearchMenage() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche le couple par numéro
		{
			Long numero = 8901L;
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(numero);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals(numero, list.get(0).getNumero());
		}

		// Recherche le mari par for principal fermé
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomey");
			criteria.setNoOfsFor("5586");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			TiersIndexedData data = list.get(0);
			assertEquals("Lausanne", data.getForPrincipal());
		}

		// Recherche le couple par for principal
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomido");
			criteria.setNoOfsFor("5652");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			TiersIndexedData data = list.get(0);
			// String nom1 = data.getNom1();
			// String nom2 = data.getNom2();
			assertEquals("Villars-sous-Yens", data.getForPrincipal());
		}

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomido");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());

			TiersIndexedData marcel = list.get(0);
			TiersIndexedData couple = list.get(1);
			if (list.get(0).getNumero().equals(Long.valueOf(8901L))) {
				marcel = list.get(1);
				couple = list.get(0);
			}

			assertEquals(Long.valueOf(7823L), marcel.getNumero());
			assertEquals("Marcel Bolomido", marcel.getNom1());
			assertEquals(Long.valueOf(8901L), couple.getNumero());
			assertEquals("Marcel Bolomido", couple.getNom1());
			assertEquals("Claudine Desplatanes", couple.getNom2());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParNumeroAVS() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Ancien numero
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("987.65.432.109");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("98765432109");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
		}

		// Nouveau numero
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("1234567891023");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("123.4567.8910.23");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
		}
	}

	private void rechercheParTypeTiers(String nom, TypeTiers type, int expected) throws Exception {

		final TiersCriteria criteria = new TiersCriteria() {
			@Override
			public boolean isEmpty() {
				// on veut pouvoir exécuter les recherches sans limitation
				return false;
			}
		};
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
		criteria.setNomRaison(nom);
		criteria.setTypeTiers(type);
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		//dumpResults(list);
		assertEquals(expected, list.size());
	}

	// Recherche sur le type de tiers
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParTypeTiers() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		rechercheParTypeTiers("despont", TypeTiers.HABITANT, 1); // Despont Alain
		rechercheParTypeTiers(null, TypeTiers.NON_HABITANT, 2);
		rechercheParTypeTiers("desp", TypeTiers.PERSONNE_PHYSIQUE, 2); // Despont Alain et Desplatanes Claudine
		rechercheParTypeTiers("desp", TypeTiers.CONTRIBUABLE, 3); // Despont Alain, Desplatanes Claudine + (Bolomido Marcel & Desplatanes Claudine)
		rechercheParTypeTiers(null, TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE, 1);
		rechercheParTypeTiers(null, TypeTiers.COLLECTIVITE_ADMINISTRATIVE, getDefaultCollectivitesAdministrativesNumber());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNatureJuridique() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Tiers par Nature Juridique
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("parnen");
			criteria.setNatureJuridique("PP");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());

			TiersIndexedData proxy1 = list.get(0);
			assertEquals(Long.valueOf(76327L), proxy1.getNumero());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParAdresse() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays("Prilly");
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(1, list.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParNpa() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		TiersCriteria criteria = new TiersCriteria();
		criteria.setNpaCourrier(String.valueOf(MockLocalite.Renens.getNPA()));
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);

		assertEquals(3, list.size());

		List<Long> ids = new ArrayList<>();
		for (TiersIndexedData data : list) {
			ids.add(data.getNumero());
		}
		assertTrue(ids.contains(Long.valueOf(8901))); // ménage commun Bolomido Marcel + Duchene Claudine -> mr non habitant le ménage prend l'adresse de mme
		assertTrue(ids.contains(Long.valueOf(7239))); // habitant Bolomey Richard
		assertTrue(ids.contains(Long.valueOf(7632))); // habitant Duchene Claudine
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheVideException() throws Exception {

		try {
			TiersCriteria criteria = new TiersCriteria();
			globalTiersSearcher.search(criteria);
			fail();
		}
		catch (Exception e) {
			assertEquals("Les critères de recherche sont vides", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParAdresseZeroTrouve() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		TiersCriteria criteria = new TiersCriteria();

		criteria.setLocaliteOuPays("Montreux"); // Ne devrait pas etre trouvé!
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEmpty(list);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParFors() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		TiersCriteria criteria = new TiersCriteria();

		// 3 tiers à Lausanne
		{
			criteria.setNoOfsFor("5586"); // Lausanne
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(3, list.size());
		}

		// 1 tiers à Cossonay
		{
			criteria.setNoOfsFor("5477"); // Cossonay
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}

		// 1 tiers à Cossonay pour ID=5434
		{
			criteria.setNumero(5434L);
			criteria.setNoOfsFor("5477"); // Cossonay
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			TiersIndexedData data = list.get(0);
			assertEquals("Romainmôtier-Envy", data.getForPrincipal());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheParDateNaissance() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche sur la date de naissance
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setDateNaissance(RegDateHelper.indexStringToDate("19421207"));

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(1, list.size());
		TiersIndexedData proxy1 = list.get(0);
		String date1 = proxy1.getDateNaissance();
		assertEquals(date1, RegDateHelper.toIndexString(RegDate.get(1942, 12, 7)));
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheNomContient() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "contient" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("pon");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);

		int z = list.size();
		assertEquals(2, z);
		TiersIndexedData tiers1 = list.get(0);
		TiersIndexedData tiers2 = list.get(1);
		if (!list.get(0).getNumero().equals(5434L)) {
			tiers1 = list.get(1);
			tiers2 = list.get(0);
		}
		assertEquals(Long.valueOf(5434L), tiers1.getNumero());
		assertEquals("Alain Despont", tiers1.getNom1());
		assertEquals(Long.valueOf(1234L), tiers2.getNumero());
		assertEquals("Alain Despont", tiers2.getNom1()); // [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheAutresNoms() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "contient" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("pon ain");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(2, list.size());

		final TiersIndexedData contribuable;
		final TiersIndexedData debiteur;
		{
			final TiersIndexedData premier = list.get(0);
			final TiersIndexedData deuxieme = list.get(1);
			contribuable = premier.getRoleLigne1().contains("Contribuable") ? premier : deuxieme;
			debiteur = (contribuable == premier ? deuxieme : premier);
		}
		assertEquals(Long.valueOf(5434), contribuable.getNumero());
		assertEquals("Alain Despont", contribuable.getNom1());
		assertEquals(Long.valueOf(1234), debiteur.getNumero());
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheNomRessemble() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "phonetique" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("despant");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.PHONETIQUE);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(2, list.size());

		final TiersIndexedData contribuable;
		final TiersIndexedData debiteur;
		{
			final TiersIndexedData premier = list.get(0);
			final TiersIndexedData deuxieme = list.get(1);
			contribuable = premier.getRoleLigne1().contains("Contribuable") ? premier : deuxieme;
			debiteur = (contribuable == premier ? deuxieme : premier);
		}
		assertEquals(Long.valueOf(5434), contribuable.getNumero());
		assertNotNull(contribuable.getNom1());
		assertEquals(Long.valueOf(1234), debiteur.getNumero());
	}

	/**
	 * Teste la recherche sur un numero.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSearchIndividuParPrenom() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);

		String prenom = "Richard";

		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(prenom);
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(1, list.size());

		TiersIndexedData proxy1 = null;
		for (TiersIndexedData v : list) {
			if (v.getNumero().equals(7239L)) {
				proxy1 = v;
			}
		}
		assertEquals(Long.valueOf(7239L), proxy1.getNumero());
		assertEquals(RegDate.get(1942, 12, 7), RegDateHelper.indexStringToDate(proxy1.getDateNaissance()));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSearchEntreprise() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(8 + getDefaultCollectivitesAdministrativesNumber(), c);
		
		Long numero = 27769L;

		// Par numero
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(numero);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals(numero, list.get(0).getNumero());
		}

		// Par Nature Juridique
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNatureJuridique("PM");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals(numero, list.get(0).getNumero());
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheTropDeResultats() throws Exception {

		// Le nombre de resultats est limité dans la recherche
		final int nbMaxParListe = new Integer(ParametreEnum.nbMaxParListe.getDefaut());
		final int nbDocs = nbMaxParListe + 20;

		final List<Long> ids = doInNewTransactionAndSession(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> ids = new ArrayList<>(2000);
				for (long i = 0; i < nbDocs; i++) {
					final PersonnePhysique pp = addNonHabitant("Bimbo", "Maluna", date(1970, 1, 1), Sexe.MASCULIN);
					ids.add(pp.getNumero());
				}
				return ids;
			}
		});

		globalTiersIndexer.schedule(ids);
		globalTiersIndexer.sync();

		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("maluNa");
		try {
			globalTiersSearcher.search(criteria);
			fail(); // renvoie trop de resultats
		}
		catch (Exception e) {
			assertContains("Le nombre max de résultats ne peut pas excéder " +
				ParametreEnum.nbMaxParListe.getDefaut() + ". Hits: " + nbDocs, e.getMessage());
		}
	}

	/**
	 * [UNIREG-2597] Vérifie que la recherche de tous les ids n'est pas limité par le paramètre maxHits.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAllIds() throws Exception {

		globalTiersIndexer.overwriteIndex();

		// Le nombre de resultats est limité dans la recherche
		final int nbMaxParListe = new Integer(ParametreEnum.nbMaxParListe.getDefaut());
		final int nbDocs = nbMaxParListe + 20;

		final Set<Long> idsDb = doInNewTransactionAndSession(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				final Set<Long> ids = new HashSet<>();
				for (long i = 0; i < nbDocs; i++) {
					PersonnePhysique pp = addNonHabitant("Alfred", "Fodor", date(1970, 1, 1), Sexe.MASCULIN);
					ids.add(pp.getNumero());
				}
				return ids;
			}
		});

		globalTiersIndexer.sync();

		// La méthode 'getAllIds' ne devrait pas être limitée par le paramètre maxHists, donc les deux ensembles d'ids devraient être égaux
		final Set<Long> idsIndexer = globalTiersSearcher.getAllIds();
		assertNotNull(idsIndexer);
		assertEquals(idsDb.size(), idsIndexer.size());
		assertEquals(idsDb, idsIndexer);
	}

	/**
	 * [UNIREG-2592] Vérifie qu'une recherche avec un nom de raison égal à un espace (" ") ne plante pas.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheNomRaisonUnEspace() throws Exception {

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(" ");

		try {
			globalTiersSearcher.search(criteria);
			fail("Les critères de recherche sont comme vides...");
		}
		catch (EmptySearchCriteriaException e) {
			// ok...
		}
	}

	/**
	 * [UNIREG-3157] Vérifie que la recherche avec le mode de visualisation limitée fonctionne correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheVisualisationLimitee()  throws Exception {

		class Ids {
			Long ramon;
			Long julien;
		}
		final Ids ids = new Ids();

		// Crée deux ctbs, dont un est un débiteur inactif
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ramon = addNonHabitant("Ramon", "Zarrate", date(1930, 3, 2), Sexe.MASCULIN);
				ramon.setDebiteurInactif(true);
				addForPrincipal(ramon, date(1980,1,1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
				addForSecondaire(ramon, date(1980, 1,1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				ids.ramon = ramon.getId();

				final PersonnePhysique julien = addNonHabitant("Julien", "Zarrate", date(1930, 3, 2), Sexe.MASCULIN);
				addForPrincipal(julien, date(1980,1,1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
				addForSecondaire(julien, date(1980, 1,1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				ids.julien = julien.getId();
				return null;
			}
		});

		globalTiersIndexer.sync();

		// Effectue une recherche avec un type de visualisation limité : seuls les débiteur actifs doivent être retournés
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeVisualisation(TiersCriteria.TypeVisualisation.LIMITEE);
		criteria.setNomRaison("Zarrate");

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("Julien Zarrate", list.get(0).getNom1());
	}

	/**
	 * Encode les 0-9 en A-J pour éviter de mettre des chiffres dans les noms/prénoms des personnes physiques
	 */
	private static String encodeDigitsInName(String originalName) {
		final StringBuilder b = new StringBuilder();
		for (char c : originalName.toCharArray()) {
			if (Character.isDigit(c)) {
				b.append((char) (c - '0' + 'A'));
			}
			else {
				b.append(c);
			}
		}
		return b.toString();
	}

	/**
	 * [UNIREG-1386] Vérifie que le moteur de recherche supprime automatiquement les termes trop communs sur le champ 'nom/raison' lorsqu'une exception BooleanQuery.TooManyClause est levée par lucene.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheCriteresTropCommuns() throws Exception {

		final List<Long> ids = doInNewTransactionAndSession(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {

				final List<Long> ids = new ArrayList<>(2000);

				// Charge 2000 personnes dans l'index. Ces 2000 personnes possèdent toutes un nom de famille commençant par "Du Pont".
				for (int i = 0; i < 2000; ++i) {

					final String nom;
					final String prenom;
					final String localite;

					if (i == 0) {
						// Cas spécial pour le premier
						nom = "Du Pont";
						prenom = "Michel";
						localite = "Romanel-s-Morges";
					}
					else {
						nom = String.format("Du Pont%04d", i); // "Du Pont0001".."Du Pont1999"
						prenom = String.format("Michel%02d", i % 50); // 40 * (Michel01..Michel49)
						localite = String.format("Romanel-s%04d-Lausanne", i); // "Romanel-s0001-Lausanne".."Romanel-s1999-Lausanne"
					}

					final PersonnePhysique pp = addNonHabitant(encodeDigitsInName(prenom), encodeDigitsInName(nom), date(1970, 1, 1), Sexe.MASCULIN);
					addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(1970,1,1), null, "chemin du devin", encodeDigitsInName(localite), MockPays.Suisse);
					ids.add(pp.getNumero());
				}
				return ids;
			}
		});

		globalTiersIndexer.schedule(ids);
		globalTiersIndexer.sync();

		// Recherche les 40 personnes nommées "MichelCC Du Pont*"
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("MichelCC Du Pont");
			criteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(40, list.size());

			// Trie par ordre des noms croissant
			Collections.sort(list, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return o1.getNom1().compareTo(o2.getNom1());
				}
			});

			int i = 0;
			for (TiersIndexedData d : list) {
				final String nomAttendu = encodeDigitsInName(String.format("MichelCC Du Pont%04d", (i++ * 50 + 22)));
				assertEquals(nomAttendu, d.getNom1());
			}
		}

		{
			// [UNIREG-2142] teste la recherche sur les localités
			final TiersCriteria criteria = new TiersCriteria();
			// le 's' vers s'étendre en 's*' -> lucene va lever une exception parce que le nombre de termes est dépassé. Unireg devrait là-dessus filtrer les termes les plus courts et re-essayer.
			criteria.setLocaliteOuPays(encodeDigitsInName("Romanel-s-Morges"));

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals("Romanel-s-Morges", d.getLocalite());
		}
	}

	@SuppressWarnings("unused")
	private void dumpResults(List<TiersIndexedData> values) {
		for (TiersIndexedData v : values) {
			System.out.println("Numero: " + v.getNumero());
			System.out.println("Nom1: " + v.getNom1());
			System.out.println("Nom2: " + v.getNom2());
			System.out.println("Date naissance: " + v.getDateNaissance());
		}
	}

	/**
	 * [SIFISC-5846] Vérifie qu'il est possible de recherche quelqu'un à partir de son ancien numéro de sourcier.
	 */
	@Test
	public void testRechercheParAncienNumeroDeSourcier() throws Exception {

		class Ids {
			long marcel;
			long jules;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique marcel = addNonHabitant("Marcel", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
				marcel.setAncienNumeroSourcier(333111L);
				ids.marcel = marcel.getId();

				final PersonnePhysique jules = addNonHabitant("Jules", "Espol", date(1936, 8, 22), Sexe.MASCULIN);
				ids.jules = jules.getId();
				return null;
			}
		});

		globalTiersIndexer.sync();

		// numéro connu
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setAncienNumeroSourcier(333111L);

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals(ids.marcel, d.getNumero().longValue());
		}

		// numéro inconnu
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setAncienNumeroSourcier(111333L);

			assertEmpty(globalTiersSearcher.search(criteria));
		}

		// pas de critère sur le numéro
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Jules");

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals(ids.jules, d.getNumero().longValue());
		}
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());

		loadDatabase(DB_UNIT_DATA_FILE);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Despont");

		final Thread fuseBlowingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					fusible.blow();
				}
				catch (InterruptedException e) {
					// on sort... de toute façon, si le <i>sleep</i> a sauté, le fusible va rester et le test partira en timeout
				}
			}
		});

		final long start = System.nanoTime();
		fuseBlowingThread.start();
		try {
			globalTiersSearcher.flowSearch(criteria, queue, fusible);
		}
		finally {
			final long end = System.nanoTime();
			assertTrue(end - start > TimeUnit.MILLISECONDS.toNanos(2000));
		}

		fuseBlowingThread.join();
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleDejaGrilleAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());
		fusible.blow();
		assertTrue(fusible.isBlown());

		loadDatabase(DB_UNIT_DATA_FILE);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Dupont");

		// fusible grillé, donc même s'il y a des résulats, on n'est pas bloqué
		globalTiersSearcher.flowSearch(criteria, queue, fusible);
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleSansResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());

		loadDatabase(DB_UNIT_DATA_FILE);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Robert Pittet");

		// pas de résultat, pas de blocage, même si la queue de sortie n'est pas consommée
		globalTiersSearcher.flowSearch(criteria, queue, fusible);
	}

	@Test(timeout = 20000)
	public void testFlowSearchAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		loadDatabase(DB_UNIT_DATA_FILE);

		final List<TiersIndexedData> found = new ArrayList<>();
		final Fuse done = new Fuse();
		final Thread listener = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (done.isNotBlown()) {
						final TiersIndexedData data = queue.poll(10, TimeUnit.MILLISECONDS);
						if (data != null) {
							found.add(data);
						}
					}
				}
				catch (InterruptedException e) {
					// on sort...
				}
			}
		});
		listener.start();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Despont");

		globalTiersSearcher.flowSearch(criteria, queue, fusible);
		done.blow();
		listener.join();

		assertEquals(2, found.size());      // une PP et un DPI
	}

	@Test
	public void testRechercheSexe() throws Exception {

		class Ids {
			long lui;
			long elle;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique elle = addNonHabitant("Albertine", "Cochin", null, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Robert", "Cochin", null, Sexe.MASCULIN);
				final Ids ids = new Ids();
				ids.elle = elle.getNumero();
				ids.lui = lui.getNumero();
				return ids;
			}
		});

		globalTiersIndexer.sync();

		// recherche elle
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			criteria.setSexe(Sexe.FEMININ);
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.elle, res.get(0).getNumero());
		}

		// recherche lui
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			criteria.setSexe(Sexe.MASCULIN);
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.lui, res.get(0).getNumero());
		}

		// recherche les deux
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});
			assertEquals((Long) ids.elle, res.get(0).getNumero());
			assertEquals((Long) ids.lui, res.get(1).getNumero());
		}
	}

	@Test
	public void testRechercheDateNaissancePartielle() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		class Ids {
			long pp1;
			long pp2;
			long pp3;
			long pp4;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp1 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965), Sexe.MASCULIN);
				final PersonnePhysique pp2 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965,2), Sexe.MASCULIN);
				final PersonnePhysique pp3 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965,2,21), Sexe.MASCULIN);
				final PersonnePhysique pp4 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965,3), Sexe.MASCULIN);

				final Ids ids = new Ids();
				ids.pp1 = pp1.getNumero();
				ids.pp2 = pp2.getNumero();
				ids.pp3 = pp3.getNumero();
				ids.pp4 = pp4.getNumero();
				return ids;
			}
		});

		final Comparator<TiersIndexedData> comparator = new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		};

		// on attend la fin de l'indexation des bonshommes
		globalTiersIndexer.sync();

		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 4, 12));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 2, 12));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 2, 21));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(3, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 4));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 3));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp4, res.get(1).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965, 2));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(3, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1965));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(4, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
			assertEquals((Long) ids.pp4, res.get(3).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissance(RegDate.get(1966));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(0, res.size());
		}
	}

}