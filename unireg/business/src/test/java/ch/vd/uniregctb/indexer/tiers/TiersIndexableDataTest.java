package ch.vd.uniregctb.indexer.tiers;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.indexer.GlobalIndex;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.SearchCallback;
import ch.vd.uniregctb.indexer.lucene.FSIndexProvider;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.NatureJuridique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TiersIndexableDataTest extends WithoutSpringTest {

	private GlobalIndex globalIndex;
	private GlobalTiersSearcherImpl globalTiersSearcher;
	private static final long ID = 1234L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		globalIndex = new GlobalIndex(new FSIndexProvider("target/index-TiersIndexableDataTest"));
		globalIndex.afterPropertiesSet();
		globalIndex.overwriteIndex();

		globalTiersSearcher = new GlobalTiersSearcherImpl();
		globalTiersSearcher.setGlobalIndex(globalIndex);
	}

	@Override
	public void onTearDown() throws Exception {
		globalIndex.destroy();
		super.onTearDown();
	}

	private static TiersIndexableData newIndexableData() {
		final TiersIndexableData data = new TiersIndexableData(ID, "test", "test");
		data.setAnnule(Boolean.FALSE);
		data.setDebiteurInactif(Boolean.FALSE);
		data.setIndexationDate(new Date().getTime());
		return data;
	}

	@Test
	public void testIndexationNumeros() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(ID);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNumero(6667L);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	/**
	 * [SIFISC-6093] Vérifie que la recherche simple sur les numéros de tiers fonctionne avec les numéros de tiers formattés (107.605.50) ou non (10760550)
	 */
	@Test
	public void testSearchTopNumeros() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = new TiersIndexableData(10760550L, "test", "test");
		data.setNumeros("10760550");
		data.setAnnule(Boolean.FALSE);
		data.setDebiteurInactif(Boolean.FALSE);
		data.setIndexationDate(new Date().getTime());
		globalIndex.indexEntity(data);

		// recherche avec numéro non-formatté
		{
			final List<TiersIndexedData> resultats = globalTiersSearcher.searchTop("10760550", null, 10);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals(Long.valueOf(10760550L), indexed.getNumero());
		}

		// recherche avec numéro formatté
		{
			final List<TiersIndexedData> resultats = globalTiersSearcher.searchTop("107.605.50", null, 10);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals(Long.valueOf(10760550L), indexed.getNumero());
		}
	}

	@Test
	public void testIndexationNomRaison() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNomRaison("Rododo");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Rododo");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNomRaison("Cradada");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationAutreNom() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setAutresNom("Rododo");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Rododo");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNomRaison("Cradada");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationDateNaissance() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.addDateNaissance(date(1975, 4, 12));
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setDateNaissanceInscriptionRC(RegDate.get(1975, 4, 12));

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals("19750412", indexed.getDateNaissanceInscriptionRC());

		// recherche des données (KO)
		criteria.setDateNaissanceInscriptionRC(RegDate.get(1988, 1, 1));
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	/**
	 * [SIFISC-5926] Vérifie que la recherche par mots-clés de la date de naissance fonctionne avec le format dd.mm.aaaa
	 */
	@Test
	public void testSearchTopDateNaissance() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.addDateNaissance(date(1975, 4, 12));
		globalIndex.indexEntity(data);

		{
			// recherche des données date non-formattée (OK)
			final List<TiersIndexedData> resultats = globalTiersSearcher.searchTop("12041975", null, 10);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
			assertEquals("19750412", indexed.getDateNaissanceInscriptionRC());
		}

		{
			// recherche des données date formattée (OK)
			final List<TiersIndexedData> resultats = globalTiersSearcher.searchTop("12.04.1975", null, 10);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
			assertEquals("19750412", indexed.getDateNaissanceInscriptionRC());
		}

		// recherche des données (KO)
		assertEmpty(globalTiersSearcher.searchTop("01.01.1988", null, 10));
	}

	@Test
	public void testIndexationNoOfsForPrincipal() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNoOfsForPrincipal(IndexerFormatHelper.numberToString(333L));
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNoOfsFor("333");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNoOfsFor("7346563");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationTypeOfsForPrincipal() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setTypeOfsForPrincipal(TypeAutoriteFiscale.COMMUNE_HC.name());
		globalIndex.indexEntity(data);

		// note : il n'y a pas de moyen de spécifier le type de for fiscal sur un TiersCriteria, on passe donc pas le global index directement

		// recherche des données (OK)
		globalIndex.search(new TermQuery(new Term(TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, TypeAutoriteFiscale.COMMUNE_HC.name())), 100, new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				assertEquals(1, hits.totalHits);
				final Document doc = docGetter.get(hits.scoreDocs[0].doc);
				assertEquals(TypeAutoriteFiscale.COMMUNE_HC.name(), doc.get(TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL));
			}
		});

		// recherche des données (KO)
		globalIndex.search(new TermQuery(new Term(TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name())), 100, new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				assertEquals(0, hits.totalHits);
			}
		});
	}

	@Test
	public void testIndexationNoOfsAutreFors() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNosOfsAutresFors("444 777");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNoOfsFor("444");

		{
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données (OK) (bis)
		criteria.setNoOfsFor("777");

		{
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données (KO)
		criteria.setNoOfsFor("7346563");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNpa() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNpaCourrier("1323");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNpaCourrier("1323");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals("1323", indexed.getNpa());

		// recherche des données (KO)
		criteria.setNpaCourrier("1322");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationLocalitePays() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setLocaliteEtPays("Paris France");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays("France");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals("Paris France", indexed.getLocaliteOuPays());

		// recherche des données (KO)
		criteria.setLocaliteOuPays("Angleterre");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNatureJuridique() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNatureJuridique(NatureJuridique.PP.name());
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNatureJuridique(NatureJuridique.PP.name());

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNatureJuridique(NatureJuridique.PM.name());
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationFormeJuridique() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setFormeJuridique(FormeLegale.N_0106_SOCIETE_ANONYME.getCode());
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setFormeJuridique(FormeJuridiqueEntreprise.SA);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setFormeJuridique(FormeJuridiqueEntreprise.SARL);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationCategorieEntreprise() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setCategorieEntreprise(CategorieEntreprise.APM.name());
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setCategorieEntreprise(CategorieEntreprise.APM);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setCategorieEntreprise(CategorieEntreprise.PM);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNumeroAssureSocial11() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNavs11("123456789");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumeroAVS("123456789");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNumeroAVS("9898989898");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNumeroAssureSocial13() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNavs13("7560000000001");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumeroAVS("7560000000001");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNumeroAVS("7560000000002");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationCategorieDebiteurIs() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setCategorieDebiteurIs(CategorieImpotSource.PRESTATIONS_PREVOYANCE.name());
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setCategorieDebiteurIs(CategorieImpotSource.PRESTATIONS_PREVOYANCE);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals(CategorieImpotSource.PRESTATIONS_PREVOYANCE, indexed.getCategorieImpotSource());

		// recherche des données (KO)
		criteria.setCategorieDebiteurIs(CategorieImpotSource.ADMINISTRATEURS);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationModeCommunicationDebiteurIs() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNumeros(String.valueOf(ID));
		data.setModeCommunication(ModeCommunication.ELECTRONIQUE);
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(ID);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals(ModeCommunication.ELECTRONIQUE, indexed.getModeCommunication());
	}

	@Test
	public void testIndexationModeImposition() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setModeImposition(ModeImposition.MIXTE_137_1.name());
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setModeImposition(ModeImposition.MIXTE_137_1);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setModeImposition(ModeImposition.MIXTE_137_2);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNoSymic() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNoSymic("343434");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNoSymic("343434");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setNoSymic("828282");
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationTiersActif() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setTiersActif(Boolean.FALSE);
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTiersActif(false);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());

		// recherche des données (KO)
		criteria.setTiersActif(true);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationAnnule() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setAnnule(Boolean.TRUE);
		data.setNomRaison("bouh");
		globalIndex.indexEntity(data);

		{
			// recherche des données (OK)
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bouh"); // autrement la TiersCriteria gueule parce que les critères sont vides
			criteria.setInclureTiersAnnules(true);

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
			assertTrue(indexed.isAnnule());

			// recherche des données (KO)
			criteria.setInclureTiersAnnules(false);
			assertEmpty(globalTiersSearcher.search(criteria));
		}

		{
			// recherche des données (OK)
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bouh"); // autrement la TiersCriteria gueule parce que les critères sont vides
			criteria.setInclureTiersAnnules(true);
			criteria.setTiersAnnulesSeulement(true);

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
			assertTrue(indexed.isAnnule());
		}
	}

	@Test
	public void testIndexationDebiteurInactif() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setDebiteurInactif(Boolean.TRUE);
		data.setNomRaison("bouh");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("bouh"); // autrement la TiersCriteria gueule parce que les critères sont vides
		criteria.setInclureI107(true);

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertTrue(indexed.isDebiteurInactif());

		// recherche des données (KO)
		criteria.setInclureI107(false);
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationMotifFermetureDernierForPrincipalAvecForFerme() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setMotifFermetureDernierForPrincipal(MotifFor.FIN_EXPLOITATION);
		globalIndex.indexEntity(data);

		// recherche des données OK
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.of(MotifFor.FIN_EXPLOITATION));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données OK
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.of(MotifFor.FIN_EXPLOITATION, MotifFor.FAILLITE));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données KO
		{
			final TiersCriteria criteria = new TiersCriteria();

			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.of(MotifFor.FAILLITE));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(0, resultats.size());
		}

		// recherche des données KO
		{
			final TiersCriteria criteria = new TiersCriteria();

			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.noneOf(MotifFor.class));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(0, resultats.size());
		}
	}

	@Test
	public void testIndexationMotifFermetureDernierForPrincipalAvecForOuvert() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setMotifFermetureDernierForPrincipal(null);
		data.setNomRaison("bouh");
		globalIndex.indexEntity(data);

		// recherche des données OK
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bouh");

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données OK
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bouh");
			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.noneOf(MotifFor.class));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData indexed = resultats.get(0);
			assertEquals((Long) ID, indexed.getNumero());
		}

		// recherche des données KO
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bouh");
			criteria.setMotifsFermetureDernierForPrincipal(EnumSet.of(MotifFor.FAILLITE));

			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(0, resultats.size());
		}
	}
}
