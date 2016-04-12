package ch.vd.uniregctb.common;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

public class MultipleSwitchTest extends WithoutSpringTest {

	private static class SimpleSwitchable implements Switchable {
		private boolean enabled;

		private SimpleSwitchable(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}
	}

	private Switchable[] buildSwitchables(boolean... state) {
		final Switchable[] array = new Switchable[state.length];
		for (int i = 0 ; i < array.length ; ++ i) {
			array[i] = new SimpleSwitchable(state[i]);
		}
		return array;
	}

	@Test
	public void testSetEnabled() throws Exception {
		final boolean[] states = {true, false, true, false, false};
		final Switchable[] array = buildSwitchables(states);
		final MultipleSwitch multiple = new MultipleSwitch(array);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		multiple.setEnabled(true);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.setEnabled(false);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertFalse("position " + i, array[i].isEnabled());
		}
	}

	@Test
	public void testPushPop() throws Exception {
		final boolean[] states = {true, false, true, false, false};
		final Switchable[] array = buildSwitchables(states);
		final MultipleSwitch multiple = new MultipleSwitch(array);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		multiple.pushState();
		multiple.setEnabled(true);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.pushState();
		multiple.setEnabled(false);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertFalse("position " + i, array[i].isEnabled());
		}
		multiple.popState();
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.popState();
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		try {
			multiple.popState();
			Assert.fail("Il y a encore des trucs dedans ?");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}
	}

	@Test
	public void testMultithread() throws Exception {

		final int NB_THREADS = 50;
		final int NB_STEPS = 1000;

		final boolean[] states = {true, false, true, false, false};
		final Switchable[] array = buildSwitchables(states);
		final MultipleSwitch multiple = new MultipleSwitch(array);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}

		final class Step implements Runnable {
			final int sleepTime;

			Step(int sleepTime) {
				this.sleepTime = sleepTime;
			}

			@Override
			public void run() {
				multiple.pushState();
				multiple.setEnabled(false);
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				finally {
					multiple.popState();
				}
			}
		}

		// lancement en multithread
		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		try {
			final Random rnd = new Random();
			final List<Future<?>> futures = new LinkedList<>();
			for (int i = 0 ; i < NB_STEPS ; ++ i) {
				futures.add(executor.submit(new Step(rnd.nextInt(100))));
			}
			executor.shutdown();

			// attente et vérification de la fin des traitements
			for (Future<?> future : futures) {
				future.get();
			}
		}
		finally {
			executor.shutdownNow();
		}
	}
}
