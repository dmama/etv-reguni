package ch.vd.uniregctb.indexer.tiers;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.GlobalIndex;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.SearchCallback;
import ch.vd.uniregctb.indexer.fs.FSDirectoryProvider;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.NatureJuridique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TiersIndexableDataTest extends WithoutSpringTest {

	private GlobalIndex globalIndex;
	private GlobalTiersSearcherImpl globalTiersSearcher;
	private static final long ID = 1234L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		globalIndex = new GlobalIndex(new FSDirectoryProvider("target/index-TiersIndexableDataTest"));
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
		data.setAnnule(IndexerFormatHelper.objectToString(false));
		data.setDebiteurInactif(IndexerFormatHelper.objectToString(false));
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
		data.setDateNaissance(IndexerFormatHelper.objectToString(RegDate.get(1975, 4, 12)));
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setDateNaissance(RegDate.get(1975, 4, 12));

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals("19750412", indexed.getDateNaissance());

		// recherche des données (KO)
		criteria.setDateNaissance(RegDate.get(1988, 1, 1));
		assertEmpty(globalTiersSearcher.search(criteria));
	}

	@Test
	public void testIndexationNoOfsForPrincipal() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNoOfsForPrincipal(IndexerFormatHelper.objectToString(333L));
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
		data.setNpa("1323");
		globalIndex.indexEntity(data);

		// recherche des données (OK)
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNpa("1323");

		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData indexed = resultats.get(0);
		assertEquals((Long) ID, indexed.getNumero());
		assertEquals("1323", indexed.getNpa());

		// recherche des données (KO)
		criteria.setNpa("1322");
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
	public void testIndexationNumeroAssureSocial() throws Exception {

		// création et indexation des données
		final TiersIndexableData data = newIndexableData();
		data.setNumeroAssureSocial("123456789");
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
		assertEquals(CategorieImpotSource.PRESTATIONS_PREVOYANCE.name(), indexed.getCategorieImpotSource());

		// recherche des données (KO)
		criteria.setCategorieDebiteurIs(CategorieImpotSource.ADMINISTRATEURS);
		assertEmpty(globalTiersSearcher.search(criteria));
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
		data.setTiersActif(IndexerFormatHelper.objectToString(false));
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
		data.setAnnule(IndexerFormatHelper.objectToString(true));
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
		data.setDebiteurInactif(IndexerFormatHelper.objectToString(true));
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
}
