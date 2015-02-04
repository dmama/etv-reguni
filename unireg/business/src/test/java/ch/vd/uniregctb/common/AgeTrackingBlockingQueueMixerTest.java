package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

public class AgeTrackingBlockingQueueMixerTest extends WithoutSpringTest {

	/**
	 * Classe de test pour fixer a priori l'âge des éléments vus par le mixer
	 */
	private static class DatedElement implements Dated {

		final long age;
		final TimeUnit unit;

		private DatedElement(long age, TimeUnit unit) {
			this.age = age;
			this.unit = unit;
		}

		@Override
		public long getAge(@NotNull TimeUnit unit) {
			return unit.convert(this.age, this.unit);
		}
	}

	@Test
	public void testSimpleAverageComputation() throws Exception {
		final BlockingQueue<DatedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<DatedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<DatedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<DatedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(3, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(1, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(5, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 5L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));
	}

	@Test
	public void testTimeUnitManipulation() throws Exception {
		final BlockingQueue<DatedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<DatedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<DatedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<DatedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		mixer.onElementOffered(new DatedElement(3000, TimeUnit.MICROSECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(1000000, TimeUnit.NANOSECONDS), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(5, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 5L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));
	}

	@Test
	public void testReset() throws Exception {
		final BlockingQueue<DatedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<DatedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<DatedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<DatedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(3, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(1, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.reset();
		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(3, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new DatedElement(1, TimeUnit.MILLISECONDS), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));
	}

	@Test
	public void testInvalidInputQueue() throws Exception {
		final BlockingQueue<DatedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<DatedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<DatedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<DatedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);
		try {
			mixer.onElementOffered(new DatedElement(3, TimeUnit.SECONDS), output);
			Assert.fail("Ce n'est pas une queue d'entrée !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Unknown input queue", e.getMessage());
		}

		try {
			mixer.getSlidingAverageAge(output);
			Assert.fail("Ce n'est pas une queue d'entrée !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Unknown input queue", e.getMessage());
		}

		try {
			mixer.getGlobalAverageAge(output);
			Assert.fail("Ce n'est pas une queue d'entrée !");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Unknown input queue", e.getMessage());
		}
	}
}
