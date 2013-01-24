package ch.vd.uniregctb.indexer.concurrent;

import org.apache.log4j.Logger;
import org.apache.lucene.search.TopDocs;

import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchCallback;

public class ConcurrentAccessSearcherThread extends AbstractConcurrentAccessThread {

	private final Logger LOGGER = Logger.getLogger(ConcurrentAccessSearcherThread.class);
	private int maxHits = 100;

	public ConcurrentAccessSearcherThread(GlobalIndexInterface globalIndex) {

		super(globalIndex);
	}

	@Override
	public void doRun() throws Exception {

		int nbSearch = 0;
		long begin = System.currentTimeMillis();

		while (!isStopPlease()) {
			globalIndex.search("Prenom:good", maxHits, new SearchCallback() {
				@Override
				public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
					int count = hits.totalHits;
					// [UNIREG-2287] Désactivé l'appel ci-dessus à cause bug de réentrance (deadlock) sur l'implémentation 'fair' en java 1.5 :
					// LOGGER.debug("S:Doc count: " + globalIndex.getApproxDocCount() + " Hits: " + count);
				}
			});
			nbSearch++;
		}
		long end = System.currentTimeMillis();
		LOGGER.warn("Time:"+(end-begin)+" Searches:"+nbSearch);
	}

}
