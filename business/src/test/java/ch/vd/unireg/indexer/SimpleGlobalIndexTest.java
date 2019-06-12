package ch.vd.unireg.indexer;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.indexer.lucene.FSIndexProvider;
import ch.vd.unireg.indexer.lucene.LuceneHelper;

import static org.junit.Assert.assertEquals;

public class SimpleGlobalIndexTest extends WithoutSpringTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(SimpleGlobalIndexTest.class);

	private static final String path = "tmp/globalIndex";
	private GlobalIndex globalIndex;
	private static final int maxHits = 100;

	private MockIndexable data = new MockIndexable(12L, "U", "a good man du", "dardare", "essuies");

	@Override
	public void onSetUp() throws Exception {

		FSIndexProvider directory = new FSIndexProvider(path);
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
		globalIndex.search(baseQuery, maxHits, (hits, docGetter) -> assertEquals(count, hits.totalHits));
	}

	@Test
	public void testPrenom() throws Exception {

		BooleanQuery booleanQuery = new BooleanQuery();
		//booleanQuery.add(LuceneHelper.getTermsExact("NomCourier", "Dardare"), BooleanClause.Occur.MUST);
		booleanQuery.add(LuceneHelper.getTermsExact("Prenom", "a"), BooleanClause.Occur.MUST);
		assertHits(1, booleanQuery);
	}

	@Test
	public void testNomCourier() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsExact("NomCourier", "Dardare"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsExact("NomCourier", "Dardar"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsCommence("NomCourier", "D", 0), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}
	}

	@Test
	public void testNom() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsExact("Nom", "U"), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsExact("Nom", "U2"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsCommence("Nom", "U", 0), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsContient("Nom", "u", 0), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}

	}

	@Test
	public void testChamp1() throws Exception {

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsExact("Champ1", "essui"), BooleanClause.Occur.MUST);
			assertHits(0, booleanQuery);
		}

		{
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(LuceneHelper.getTermsCommence("Champ1", "essui", 0), BooleanClause.Occur.MUST);
			assertHits(1, booleanQuery);
		}
	}

}
