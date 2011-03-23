package ch.vd.uniregctb.indexer.concurrent;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.MockIndexable;

public class ConcurrentAccessIndexerThread extends AbstractConcurrentAccessThread {

	private final Logger LOGGER = Logger.getLogger(ConcurrentAccessIndexerThread.class);

	public ConcurrentAccessIndexerThread(GlobalIndexInterface globalIndex) {

		super(globalIndex);
	}

	public void onsetUp() throws Exception {
		globalIndex.overwriteIndex();
	}

	@Override
	protected void doRun() throws Exception {

		int nbDocs = 0;
		while (!isStopPlease()) {

			nbDocs++;

			MockIndexable data = new MockIndexable((long)nbDocs, "U", "a good man du", "dardare", "essuies");
			globalIndex.indexEntity(data);
			LOGGER.debug("I:Doc count: "+globalIndex.getApproxDocCount());

			// Sleep de 1 ms permet de donner la main a une autre Thread
			Thread.sleep(1);
		}
		LOGGER.warn("Doc count: "+globalIndex.getApproxDocCount());
	}

}
