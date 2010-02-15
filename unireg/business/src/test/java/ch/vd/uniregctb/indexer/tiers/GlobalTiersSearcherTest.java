package ch.vd.uniregctb.indexer.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;

/**
 * Classe pour tester la recherche de tiers
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 */
public class GlobalTiersSearcherTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(GlobalTiersSearcherTest.class);

	private static final String DB_UNIT_DATA_FILE = "GlobalTiersSearcherTest.xml";

	public GlobalTiersSearcherTest() {
		setWantIndexation(true);
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
				MockIndividu alain = addIndividu(9876, RegDate.get(1976, 2, 27), "Dupont", "Alain", true);
				MockIndividu richard = addIndividu(9734, RegDate.get(1942, 12, 7), "Bolomey", "Richard", true);
				MockIndividu james = addIndividu(1373, RegDate.get(1992, 1, 14), "Dean", "James", true);
				MockIndividu francois = addIndividu(403399, RegDate.get(1961, 3, 12), "Lestourgie", "Francois", true);
				MockIndividu claudine = addIndividu(222, RegDate.get(1975, 11, 30), "Duchene", "Claudine", false);
				MockIndividu alain2 = addIndividu(111, RegDate.get(1965, 05, 21), "Dupont", "Alain", true);
				MockIndividu miro = addIndividu(333, RegDate.get(1972, 07, 15), "Boillat dupain", "Miro", true);
				MockIndividu claudine2 = addIndividu(444, RegDate.get(1922, 02, 12), "Duchene", "Claudine", false);

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
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, null, null, "1234", "Mock Localite " + new Date(), "4848", RegDate.get(1980, 11, 2), null);
				addAdresse(individu, EnumTypeAdresse.COURRIER, null, null, "4567", "Mock Localite " + new Date(), "5252", RegDate.get(1980, 11, 2), null);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
		int c = globalTiersSearcher.getGlobalIndex().getExactDocCount();
		assertEquals(8, c);
	}

	@Test
	public void testRechercheParNumeroContribuable() {
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
			criteria.setNumero(1234456L); // Inexistant
			criteria.setNomRaison("Bolomido"); // Bolomido n'est pas utilisé
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
	}

	@Test
	public void testSearchMenage() throws Exception {

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
			assertEquals("Bolomido Marcel", marcel.getNom1());
			assertEquals(Long.valueOf(8901L), couple.getNumero());
			assertEquals("Bolomido Marcel", couple.getNom1());
			assertEquals("Duchene Claudine", couple.getNom2());
		}
	}

	@Test
	public void testRechercheParNumeroAVS() {

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

	/**
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	private void rechercheParTypeTiers(String nom, TypeTiers type, int expected) throws Exception {

		TiersCriteria criteria = new TiersCriteria() {
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
	public void testRechercheParTypeTiers() throws Exception {

		rechercheParTypeTiers("dupont", TypeTiers.HABITANT, 1);
		rechercheParTypeTiers(null, TypeTiers.NON_HABITANT, 2);
		rechercheParTypeTiers("du", TypeTiers.PERSONNE_PHYSIQUE, 2); // Dupont Alain et Duchene Claudine
		rechercheParTypeTiers("du", TypeTiers.CONTRIBUABLE, 3); // Dupont Alain, Duchene Claudine + (Bolomido Marcel & Duchene Claudine)
		rechercheParTypeTiers(null, TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE, 1);
	}

	@Test
	public void testNatureJuridique() throws Exception {

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

	/**
	 * @throws Exception
	 */
	@Test
	public void testRechercheParAdresse() throws Exception {

		TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays("Lausanne");
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(1, list.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRechercheParNpa() throws Exception {

		TiersCriteria criteria = new TiersCriteria();
		criteria.setNpa("4567");
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);

		assertEquals(3, list.size());

		List<Long> ids = new ArrayList<Long>();
		for (TiersIndexedData data : list) {
			ids.add(data.getNumero());
		}
		assertTrue(ids.contains(Long.valueOf(8901))); // ménage commun Bolomido Marcel + Duchene Claudine -> mr non habitant le ménage prend l'adresse de mme
		assertTrue(ids.contains(Long.valueOf(7239))); // habitant Bolomey Richard
		assertTrue(ids.contains(Long.valueOf(7632))); // habitant Duchene Claudine
	}

	/**
	 * @throws Exception
	 */
	@Test
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

	/**
	 * @throws Exception
	 */
	@Test
	public void testRechercheParAdresseZeroTrouve() throws Exception {

		TiersCriteria criteria = new TiersCriteria();

		criteria.setLocaliteOuPays("Montreux"); // Ne devrait pas etre trouvé!
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEmpty(list);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRechercheParFors() throws Exception {

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

	/**
	 * @throws Exception
	 */
	@Test
	public void testRechercheParDateNaissance() throws Exception {

		// Recherche sur la date de naissance
		TiersCriteria criteria = new TiersCriteria();
		criteria.setDateNaissance(RegDateHelper.indexStringToDate("19421207"));
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		//dumpResults(list);
		assertEquals(1, list.size());
		TiersIndexedData proxy1 = list.get(0);
		String date1 = proxy1.getDateNaissance();
		assertEquals(date1, RegDateHelper.toIndexString(RegDate.get(1942, 12, 7)));
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRechercheNomContient() throws Exception {
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
		assertEquals("Dupont Alain", tiers1.getNom1());
		assertEquals(Long.valueOf(1234L), tiers2.getNumero());
		assertEquals("Dupont Alain", tiers2.getNom1()); // [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
	}

	@Test
	public void testRechercheAutresNoms() throws Exception {
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
		assertEquals("Dupont Alain", contribuable.getNom1());
		assertEquals(Long.valueOf(1234), debiteur.getNumero());
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRechercheNomRessemble() throws Exception {
		// Recherche "phonetique" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria = new TiersCriteria();
		criteria.setNomRaison("dupant");
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
	 *
	 * @throws Exception
	 */
	@Test
	public void testSearchIndividuParPrenom() throws Exception {

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
	public void testSearchEntreprise() throws Exception {

		Long numero = 63427L;

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

	@Test
	public void testRechercheTropDeResultats() throws Exception {

		// Le nombre de resultats est limité dans la recherche
		Integer nbMaxParListe = new Integer(ParametreEnum.nbMaxParListe.getDefaut());
		int nbDocs = nbMaxParListe.intValue() + 20;
		for (long i = 0; i < nbDocs; i++) {
			PersonnePhysique nonHab = new PersonnePhysique(false);
			nonHab.setNumero(i);
			nonHab.setNom("Maluna");
			nonHab.setPrenom("Bimbo");
			globalTiersIndexer.indexTiers(nonHab, false);
		}

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
	 * [UNIREG-1386] Vérifie que le moteur de recherche supprime automatiquement les termes trop communs lorsqu'une exception
	 * BooleanQuery.TooManyClause est levée par lucene.
	 */
	@Test
	public void testRechercheCriteresTropCommuns() {

		// Charge 2000 personnes dans l'index. Ces 2000 personnes possèdent toutes un nom de famille commençant par "Du Pont".
		for (int i = 1; i < 2000; ++i) {

			final String nom = "Du Pont" + i; // "Du Pont0".."Du Pont1999"
			final String prenom = "Michel" + String.valueOf(i % 50); // 40 * (Michel0..Michel49)

			PersonnePhysique nonHab = new PersonnePhysique(false);
			nonHab.setNumero((long)i);
			nonHab.setNom(nom);
			nonHab.setPrenom(prenom);

			globalTiersIndexer.indexTiers(nonHab, false);
		}

		// Recherche les 40 personnes nommées "Michel22 Du Pont*"
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Michel22 Du Pont");
		criteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertNotNull(list);
		assertEquals(40, list.size());

		int i = 0;
		for (TiersIndexedData d : list) {
			assertEquals("Du Pont" + (i++ * 50 + 22) + " Michel22", d.getNom1());
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

}
