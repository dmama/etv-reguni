package ch.vd.uniregctb.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class StackedThreadLocalTest extends WithoutSpringTest {

	@Test
	public void testPushPop() {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);
		tl.pushState();
		tl.get().add(42L);
		tl.pushState();
		Assert.assertEquals(Collections.emptySet(), tl.get());
		tl.popState();
		Assert.assertEquals(Collections.singleton(42L), tl.get());
		tl.popState();
		Assert.assertEquals(Collections.emptySet(), tl.get());
	}

	@Test
	public void testMultithreadedPushPop() throws Exception {
		final int nbThreads = 50;
		final int nbTasks = 500;
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);

		final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		try {
			final ExecutorCompletionService<Long> service = new ExecutorCompletionService<>(executor);
			for (int i = 0 ; i < nbTasks ; ++ i) {
				final long loopIndex = i;
				service.submit(() -> {
					tl.pushState();
					tl.get().add(loopIndex);
					tl.pushState();
					Assert.assertEquals(Long.toString(loopIndex), Collections.emptySet(), tl.get());
					tl.popState();
					Assert.assertEquals(Long.toString(loopIndex), Collections.singleton(loopIndex), tl.get());
					tl.popState();
					Assert.assertEquals(Long.toString(loopIndex), Collections.emptySet(), tl.get());
					return loopIndex;
				});
			}

			// fin de la saison des semailles ...
			executor.shutdown();

			// ... il est temps de récolter...
			int remainingTasks = nbTasks;
			do {
				final Future<Long> future = service.poll(1, TimeUnit.SECONDS);
				if (future != null) {
					-- remainingTasks;
					future.get();           // ça explose si un assert avait explosé
				}
			}
			while (remainingTasks > 0);
		}
		finally {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	public void testPopDeTrop() throws Exception {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);
		try {
			tl.popState();
			Assert.fail("Aurait dû exploser car il n'y a pas eu de push pour ce pop...");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Cannot pop last state!!", e.getMessage());
		}
	}

	@Test
	public void testSet() throws Exception {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);
		final Set<Long> data = Collections.singleton(42L);
		tl.set(data);
		tl.pushState();
		tl.popState();
		final Set<Long> set = tl.get();
		Assert.assertSame(data, set);
	}

	@Test
	public void testSetNull() throws Exception {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);
		tl.set(null);
		tl.pushState();
		tl.popState();
		final Set<Long> set = tl.get();
		Assert.assertNull(set);
	}

	@Test
	public void testDefaultConstructor() throws Exception {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>();
		final Set<Long> set = tl.get();
		Assert.assertNull(set);
	}

	@Test
	public void testReset() throws Exception {
		final StackedThreadLocal<Set<Long>> tl = new StackedThreadLocal<>(HashSet::new);
		final Set<Long> init = tl.get();
		Assert.assertNotNull(init);
		Assert.assertEquals(0, init.size());

		tl.set(null);
		final Set<Long> set = tl.get();
		Assert.assertNull(set);

		tl.reset();
		final Set<Long> reset = tl.get();
		Assert.assertNotNull(reset);
		Assert.assertEquals(0, reset.size());
		Assert.assertNotSame(init, reset);
	}
}
