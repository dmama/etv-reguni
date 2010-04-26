package ch.vd.uniregctb.indexer;

import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.indexer.fs.FSDirectoryProvider;

import static junit.framework.Assert.assertEquals;

public class SimpleGlobalIndexTest extends WithoutSpringTest {

	//private static final Logger LOGGER = Logger.getLogger(SimpleGlobalIndexTest.class);

	private final String path = "tmp/globalIndex";
	private GlobalIndex globalIndex;

	private MockIndexable data = new MockIndexable(12L, "U", "a good man du", "dardare", "essuies");

	@Override
	public void onSetUp() throws Exception {

		FSDirectoryProvider directory = new FSDirectoryProvider(path);
		globalIndex = new GlobalIndex(directory);
		globalIndex.afterPropertiesSet();
		globalIndex.overwriteIndex();

		globalIndex.indexEntity(data);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
		globalIndex.destroy();
	}

	private void assertHits(final int count, Query baseQuery) {
		globalIndex.search(baseQuery, new SearchCallback() {
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				assertEquals(count, hits.size());
			}
		});
	}

	@Test
	public void testPrenom() throws Exception {

		BooleanQuery booleanQuery = new BooleanQuery();
		//booleanQuery.add(LuceneEngine.getTermsExact("NomCourier", "Dardare"), BooleanClause.Occur.MUST);
		booleanQuery.add(LuceneEngine.getTermsExact("Prenom", "a"), BooleanClause.Occur.MUST);
		assertHits(1, booleanQuery);
	}

	@Test
	public void testNomCourier() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsExact("NomCourier", "Dardare"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsExact("NomCourier", "Dardar"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsCommence("NomCourier", "D"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}
	}

	@Test
	public void testNom() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsExact("Nom", "U"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsExact("Nom", "U2"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsCommence("Nom", "U"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsContient("Nom", "u", 0), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

	}

	@Test
	public void testChamp1() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsExact("Champ1", "essui"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneEngine.getTermsCommence("Champ1", "essui"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}
	}

}
