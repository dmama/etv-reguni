package ch.vd.uniregctb.indexer.async;


public class AsyncTiersIndexerThread extends Thread {

	//private static final Logger LOGGER = Logger.getLogger(AsyncTiersIndexerThread.class);

	private final AsyncTiersIndexer asyncIndexer;

	private long executionTime = 0;

	public AsyncTiersIndexerThread(AsyncTiersIndexer indexer) {
		this.asyncIndexer = indexer;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		asyncIndexer.delegateRun();
		executionTime = System.nanoTime() - start;
	}

	/**
	 * @return le temps d'exécution en nano-secondes
	 */
	public long getExecutionTime() {
		return executionTime;
	}
}
