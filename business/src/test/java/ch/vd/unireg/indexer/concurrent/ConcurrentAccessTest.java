package ch.vd.unireg.indexer.concurrent;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.indexer.GlobalIndexInterface;

import static org.junit.Assert.assertFalse;

public class ConcurrentAccessTest extends BusinessTest {

	private final Logger LOGGER = LoggerFactory.getLogger(ConcurrentAccessTest.class);

	private GlobalIndexInterface globalIndex;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		globalIndex = getBean(GlobalIndexInterface.class, "globalTiersIndex");
		globalIndex.overwriteIndex();
	}

	/**
	 * Cette methode insère des Document et fait des recherches en meme temps.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConcurrentAccess() throws Exception {

		ConcurrentAccessIndexerThread thread1 = new ConcurrentAccessIndexerThread(globalIndex);
		ConcurrentAccessSearcherThread thread2 = new ConcurrentAccessSearcherThread(globalIndex);

		LOGGER.info("Starting...");

		thread1.start();
		thread2.start();

		int waitTime = 10;
		LOGGER.info("Attente de "+waitTime+" secondes");
		Thread.sleep(waitTime*1000);

		thread1.stopPlease();
		thread2.stopPlease();

		thread1.join();
		assertFalse(thread1.isInError());
		thread2.join();
		assertFalse(thread2.isInError());

		LOGGER.info("Wait of "+waitTime+"[secs] finished. "+globalIndex.getApproxDocCount()+" documents in the index");
	}
}
