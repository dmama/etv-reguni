package ch.vd.uniregctb.indexer.concurrent;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Hits;

import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchCallback;

public class ConcurrentAccessSearcherThread extends AbstractConcurrentAccessThread {

	private final Logger LOGGER = Logger.getLogger(ConcurrentAccessSearcherThread.class);

	public ConcurrentAccessSearcherThread(GlobalIndexInterface globalIndex) {

		super(globalIndex);
	}

	@Override
	public void doRun() throws Exception {

		int nbSearch = 0;
		long begin = System.currentTimeMillis();

		while (!isStopPlease()) {
			globalIndex.search("Prenom:good", new SearchCallback() {
				public void handle(Hits hits) throws Exception {
					int count = hits.length();
					LOGGER.debug("S:Doc count: " + globalIndex.getApproxDocCount() + " Hits: " + count);
				}
			});
			nbSearch++;
		}
		long end = System.currentTimeMillis();
		LOGGER.warn("Time:"+(end-begin)+" Searches:"+nbSearch);
	}

}
