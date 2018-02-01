package ch.vd.unireg.common;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import org.junit.Test;

public class AgeTrackingBlockingQueueMixerTest extends WithoutSpringTest {

	/**
	 * Classe de test pour fixer a priori l'âge des éléments vus par le mixer
	 */
	private static class AgedElement implements Aged {

		final Duration age;

		private AgedElement(Duration age) {
			this.age = age;
		}

		@Override
		public Duration getAge() {
			return age;
		}
	}

	@Test
	public void testSimpleAverageComputation() throws Exception {
		final BlockingQueue<AgedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<AgedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<AgedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<AgedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(3)), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(1)), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(5)), input);
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
		final BlockingQueue<AgedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<AgedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<AgedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<AgedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(3)), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofNanos(1000000)), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(5)), input);
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
		final BlockingQueue<AgedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<AgedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<AgedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<AgedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);

		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(3)), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(1)), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.onClockChimes();
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));

		mixer.reset();
		Assert.assertNull(mixer.getSlidingAverageAge(input));
		Assert.assertNull(mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(3)), input);
		Assert.assertEquals((Long) 3L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 3L, mixer.getGlobalAverageAge(input));

		mixer.onElementOffered(new AgedElement(Duration.ofMillis(1)), input);
		Assert.assertEquals((Long) 2L, mixer.getSlidingAverageAge(input));
		Assert.assertEquals((Long) 2L, mixer.getGlobalAverageAge(input));
	}

	@Test
	public void testInvalidInputQueue() throws Exception {
		final BlockingQueue<AgedElement> input = new LinkedBlockingQueue<>();
		final BlockingQueue<AgedElement> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<AgedElement>> inputQueues = new ArrayList<>();
		inputQueues.add(input);

		final AgeTrackingBlockingQueueMixer<AgedElement> mixer = new AgeTrackingBlockingQueueMixer<>(inputQueues, output, 1, 5);
		try {
			mixer.onElementOffered(new AgedElement(Duration.ofSeconds(3)), output);
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
