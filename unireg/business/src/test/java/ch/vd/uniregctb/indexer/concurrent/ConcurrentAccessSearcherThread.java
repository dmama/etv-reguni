package ch.vd.uniregctb.indexer.concurrent;

import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.DocHit;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchCallback;
import org.apache.log4j.Logger;

import java.util.List;

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
				public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
					int count = hits.size();
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
