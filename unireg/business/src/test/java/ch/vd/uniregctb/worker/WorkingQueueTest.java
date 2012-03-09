package ch.vd.uniregctb.worker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkingQueueTest {

	static {
		try {
			Log4jConfigurer.initLogging(ResourceUtils.getURL("classpath:ut/log4j.xml").toString());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testOneSimpleWorker() throws Exception {

		final AtomicInteger call = new AtomicInteger(0);
		final AtomicInteger count = new AtomicInteger(0);

		final WorkingQueue<Integer> queue = new WorkingQueue<Integer>(20, 1, new SimpleWorker<Integer>() {
			@Override
			public void process(Integer data) throws Exception {
				call.incrementAndGet();
				count.addAndGet(data);
			}

			@Override
			public String getName() {
				return "testOneSimpleWorker";
			}
		});

		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.start();
		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.put(3);
		queue.sync();
		assertEquals(1, call.get());
		assertEquals(3, count.get());

		queue.put(1);
		queue.put(0);
		queue.put(5);
		queue.shutdown();
		assertEquals(4, call.get());
		assertEquals(9, count.get());
	}

	@Test
	public void testMultipleSimpleWorkers() throws Exception {

		final AtomicInteger call = new AtomicInteger(0);
		final AtomicInteger count = new AtomicInteger(0);

		final WorkingQueue<Integer> queue = new WorkingQueue<Integer>(20, 5, new SimpleWorker<Integer>() {
			@Override
			public void process(Integer data) throws Exception {
				call.incrementAndGet();
				count.addAndGet(data);
			}

			@Override
			public String getName() {
				return "testMultipleSimpleWorkers";
			}
		});

		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.start();
		assertEquals(0, call.get());
		assertEquals(0, count.get());

		for (int i = 0; i < 100; ++i) {
			queue.put(i);
		}
		queue.sync();
		assertEquals(100, call.get());
		assertEquals(4950, count.get());

		queue.put(1);
		queue.put(0);
		queue.put(5);
		queue.sync();
		assertEquals(103, call.get());
		assertEquals(4956, count.get());
	}

	@Test
	public void testOneBatchWorker() throws Exception {

		final AtomicInteger call = new AtomicInteger(0);
		final AtomicInteger count = new AtomicInteger(0);

		final WorkingQueue<Integer> queue = new WorkingQueue<Integer>(20, 1, new BatchWorker<Integer>() {
			@Override
			public void process(List<Integer> data) throws Exception {
				call.incrementAndGet();
				for (Integer i : data) {
					count.addAndGet(i);
				}
			}

			@Override
			public String getName() {
				return "testOneBatchWorker";
			}

			@Override
			public int maxBatchSize() {
				return 5;
			}
		});

		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.start();
		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.put(3);
		queue.sync();
		assertEquals(1, call.get());
		assertEquals(3, count.get());

		queue.put(1);
		queue.put(0);
		queue.put(5);
		queue.shutdown();
		assertEquals(2, call.get());
		assertEquals(9, count.get());
	}

	@Test
	public void testMultipleBatchWorkers() throws Exception {

		final AtomicInteger call = new AtomicInteger(0);
		final AtomicInteger count = new AtomicInteger(0);

		final WorkingQueue<Integer> queue = new WorkingQueue<Integer>(20, 5, new BatchWorker<Integer>() {
			@Override
			public void process(List<Integer> data) throws Exception {
				call.incrementAndGet();
				for (Integer i : data) {
					count.addAndGet(i);
				}
			}

			@Override
			public int maxBatchSize() {
				return 5;
			}

			@Override
			public String getName() {
				return "testMultipleBatchWorkers";
			}
		});

		assertEquals(0, call.get());
		assertEquals(0, count.get());

		queue.start();
		assertEquals(0, call.get());
		assertEquals(0, count.get());

		for (int i = 0; i < 100; ++i) {
			queue.put(i);
		}
		queue.sync();
		assertTrue(20 <= call.get() && call.get() <= 100); // entre 20 et 100 appels (respectivement 5 et 1 éléments par batch) en fonction de la distribution des lots
		assertEquals(4950, count.get());

		queue.put(1);
		queue.put(0);
		queue.put(5);
		queue.sync();
		assertTrue(21 <= call.get() && call.get() <= 103);
		assertEquals(4956, count.get());
	}

	@Test
	public void testReset() throws Exception {

		final AtomicInteger call = new AtomicInteger(0);
		final AtomicInteger count = new AtomicInteger(0);

		final WorkingQueue<Integer> queue = new WorkingQueue<Integer>(20000, 1, new SimpleWorker<Integer>() {
			@Override
			public void process(Integer data) throws Exception {
				call.incrementAndGet();
				count.addAndGet(data);
				Thread.sleep(10);
			}

			@Override
			public String getName() {
				return "testOneSimpleWorker";
			}
		});

		assertEquals(0, call.get());
		assertEquals(0, count.get());

		// on ajoute assez de données pour le faire travailler de longues minutes
		for (int i = 0; i < 10000; ++i) {
			queue.put(i);
		}

		// on démarre les threads
		queue.start();
		Thread.sleep(20);

		// on reset les threads
		queue.reset();

		// on vérifie que seulement une partie (et pas toute !) des données a été processée
		assertTrue(0 < call.get() && call.get() < 10000);
	}
}
