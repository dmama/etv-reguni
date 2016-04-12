package ch.vd.uniregctb.indexer.perf;

import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.SearchCallback;

import static org.junit.Assert.assertEquals;

public class IndexerPerformanceTest extends BusinessTest {

	private final Logger LOGGER = LoggerFactory.getLogger(IndexerPerformanceTest.class);

	private GlobalIndexInterface globalIndex;
	private static final int maxHits = 100;

	private static class Data extends IndexableData {

		private String nom;
		private String prenom;
		private String date;

		private Data(Long id, String type, String subType, String nom, String prenom, String date) {
			super(id, type, subType);
			this.nom = nom;
			this.prenom = prenom;
			this.date = date;
		}

		@Override
		public String getSubType() {
			return subType;
		}

		@Override
		public Document asDoc() {
			Document d = super.asDoc();

			d.add(new TextField("NOM", nom, Field.Store.YES));
			d.add(new TextField("PRENOM", prenom, Field.Store.YES));
			d.add(new TextField("DATE", date, Field.Store.YES));

			return d;
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPerf() throws Exception {

		boolean runTestPerf = false;
		if (runTestPerf) {
			runHard();
		}
	}

	private void runHard() throws Exception {

		globalIndex = getBean(GlobalIndexInterface.class, "globalTiersIndex");
		globalIndex.overwriteIndex();

		long begin = System.currentTimeMillis();

		int nbDocs = 10000;
		{

			String type = "perfi";

			LOGGER.info("Begin indexing of "+nbDocs+" documents");

			for (int i=0;i<nbDocs;i++) {
				Data d = new Data((long) i, type, null, "Duchmol-"+(i%450), "Christian-"+(i%220), DateHelper.dateToIndexString(getDate(i)));
				globalIndex.indexEntity(d);

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
		globalIndex.search(query, maxHits, new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
//				try {
//					for (DocHit hit : hits) {
//						Document doc = docGetter.get(hit.doc);
//						String id = doc.get(LuceneHelper.F_DOCID);
//						String type = doc.get(LuceneHelper.F_DOCTYPE);
//						String entityId = doc.get(LuceneHelper.F_ENTITYID);
//						String nom = doc.get("hostindividuNOM");
//						// String descr = h.get("DESCR");
//						hit = null;
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				assertEquals(count, hits.totalHits);
			}
		});
	}
}
