package ch.vd.uniregctb.indexer.perf;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Hits;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.GenericIndexable;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.LuceneEngine;
import ch.vd.uniregctb.indexer.SearchCallback;

public class IndexerPerformanceTest extends BusinessTest {

	private final Logger LOGGER = Logger.getLogger(IndexerPerformanceTest.class);

	private GlobalIndexInterface globalIndex;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testPerf() throws Exception {

		boolean runTestPerf = false;
		if (runTestPerf) {
			runHard();
		}
	}

	private void runHard() throws Exception {

		globalIndex = getBean(GlobalIndexInterface.class, "globalIndex");
		globalIndex.overwriteIndex();

		long begin = System.currentTimeMillis();

		int nbDocs = 10000;
		{

			String type = "perfi";
			List<String> fields = new ArrayList<String>();
			fields.add("NOM");
			fields.add("PRENOM");
			fields.add("DATE");

			LOGGER.info("Begin indexing of "+nbDocs+" documents");

			List<String> values;
			for (int i=0;i<nbDocs;i++) {
				values = new ArrayList<String>();
				values.add("Duchmol-"+(i%450));
				values.add("Christian-"+(i%220));
				values.add(DateHelper.dateToIndexString(getDate(i)));
				GenericIndexable indexable = new GenericIndexable(i, type, fields, values);
				globalIndex.indexEntity(new IndexableData(indexable));

				if (i % 1000 == 0) {
					LOGGER.info("Indexation: "+i);
				}
			}
			LOGGER.info("Nombre de docs: "+globalIndex.getApproxDocCount());
		}

		long end = System.currentTimeMillis();
		long indexTime = end-begin;
		LOGGER.info("Temps d'indexation: "+indexTime);

		begin = System.currentTimeMillis();

		for (int i=0;i<100;i++) {

			{
				String query = "NOM:duchmol-"+i;
				assertHits(23, query);
			}

			{
				String query = "PRENOM:christian*";
				assertHits(nbDocs, query);
			}

			{
				String query = "PRENOM:christian-2*";
				assertHits(1406, query);
			}

			{
				String query = "DATE:"+DateHelper.dateToIndexString(getDate(i));
				assertHits(19, query);
			}

			if (i % 100 == 0) {
				LOGGER.info("Search: "+i);
			}
		}

		end = System.currentTimeMillis();
		long searchTime = end-begin;
		LOGGER.info("Temps: "+indexTime+" / "+searchTime);
		end = 0L;
	}

	private Date getDate(int i) {
		return DateHelper.getDate(1950+(i % 50), 1+(i % 11), 1+(i % 25));
	}

	private void assertHits(final int count, String query) {
		globalIndex.search(query, new SearchCallback() {
			public void handle(Hits hits) throws Exception {
				LuceneEngine.debugHits(hits);
				assertEquals(count, hits.length());
			}
		});
	}
}
