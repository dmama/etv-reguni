package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class BlockingQueueMixerTest extends WithoutSpringTest {

	@Test
	public void testOneToOne() throws Exception {

		final BlockingQueue<Integer> input = new LinkedBlockingQueue<>();
		final BlockingQueue<Integer> output = new LinkedBlockingQueue<>();
		final List<BlockingQueue<Integer>> inputList = new ArrayList<>();
		inputList.add(input);

		final BlockingQueueMixer<Integer> mixer = new BlockingQueueMixer<>(inputList, output);
		mixer.start("Mixer");
		try {
			final Random rnd = new Random();
			final int number = rnd.nextInt();
			input.add(number);
			Thread.sleep(100);      // pour laisser le temps à l'élément de passer dans l'espace de transit (dans certains cas, mixer.size() peut le compter à double)
			Assert.assertEquals(1, mixer.size());
			final Integer found = output.poll(1000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull(found);
			Assert.assertEquals((Integer) number, found);
		}
		finally {
			mixer.stop();
		}
	}

	@Test
	public void testTwoToOne() throws Exception {

		final List<BlockingQueue<Integer>> inputList = new ArrayList<>();
		inputList.add(new LinkedBlockingQueue<>());
		inputList.add(new LinkedBlockingQueue<>());

		final BlockingQueue<Integer> output = new LinkedBlockingQueue<>();

		final BlockingQueueMixer<Integer> mixer = new BlockingQueueMixer<>(inputList, output);
		mixer.start("Mixer");
		try {

			// filling input queues
			final int nbElts = 1000;
			for (int i = 0 ; i < nbElts; ++ i) {
				final BlockingQueue<Integer> queue = inputList.get(i % 2);
				queue.add(i);
			}

			// reading from output queues
			final Set<Integer> gottenElts = new HashSet<>(nbElts);
			for (int i = 0 ; i < nbElts; ++ i) {
				final Integer found = output.poll(1000, TimeUnit.MILLISECONDS);
				Assert.assertNotNull(found);
				Assert.assertFalse(Integer.toString(found), gottenElts.contains(found));
				gottenElts.add(found);
			}

			// nothing more should come out afterwards
			final Integer after = output.poll(1000, TimeUnit.MILLISECONDS);
			Assert.assertNull(after);
		}
		finally {
			mixer.stop();
		}
	}

	@Test(timeout = 10000L)
	public void testPriorisation() throws Exception {

		final BlockingQueue<Integer> autobahn = new LinkedBlockingQueue<>();
		final BlockingQueue<Integer> landstrasse = new LinkedBlockingQueue<>();
		final List<BlockingQueue<Integer>> inputList = new ArrayList<>();
		inputList.add(autobahn);
		inputList.add(landstrasse);

		final BlockingQueue<Integer> output = new SynchronousQueue<>(true);

		final BlockingQueueMixer<Integer> mixer = new BlockingQueueMixer<>(inputList, output);
		mixer.start("Mixer");
		try {
			final int base = 1000;
			final int quickone = base - 1;
			final int nbEltsAutobahn = 10000;

			// many elements drive on the autobahn until the output way is completely congested
			for (int i = 0 ; i < nbEltsAutobahn ; ++ i) {
				autobahn.add(base + i);
			}
			// this one is not coming through the autobahn... but should be quicker than most due to the congestion
			landstrasse.add(quickone);
			Thread.sleep(10);       // <-- on attend un peu pour stabiliser tout ça
			Assert.assertEquals(nbEltsAutobahn + 1, mixer.size());

			Integer indexFound = null;
			int index = 0;
			while (true) {
				final Integer found = output.poll(1000, TimeUnit.MILLISECONDS);
				Assert.assertNotNull(found);
				if (found == quickone) {
					indexFound = index;
					break;
				}
				++ index;

				// simulation d'un léger temps de traitement
				Thread.sleep(10);
			}
			Assert.assertNotNull(indexFound);
			Assert.assertTrue(Integer.toString(indexFound), indexFound < 30);
		}
		finally {
			mixer.stop();
		}
	}

}
