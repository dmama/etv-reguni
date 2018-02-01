package ch.vd.unireg.common;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Active object which takes elements from several input queues and transfers them into a single ouput queue
 * @param <T> type of the managed elements
 */
public class BlockingQueueMixer<T> {

	private static final Duration POLL_TIMEOUT = Duration.ofMillis(100);
	private static final Duration OFFER_TIMEOUT = Duration.ofMillis(100);

	private static final Logger LOGGER = LoggerFactory.getLogger(BlockingQueueMixer.class);

	private final List<BlockingQueue<T>> input;
	private final BlockingQueue<T> output;
	private final List<DispatchingThread> workers;

	public BlockingQueueMixer(List<BlockingQueue<T>> inputQueues, BlockingQueue<T> outputQueue) {
		this.input = new ArrayList<>(inputQueues);
		this.output = outputQueue;
		this.workers = new ArrayList<>(inputQueues.size());
	}

	public void start(String threadNamePrefix) {
		start(new DefaultThreadNameGenerator(threadNamePrefix));
	}

	public synchronized void start(ThreadNameGenerator threadNameGenerator) {
		if (this.workers.size() > 0) {
			throw new IllegalStateException("Already started!");
		}
		for (BlockingQueue<T> in : this.input) {
			this.workers.add(new DispatchingThread(threadNameGenerator.getNewThreadName(), in, this.output));
		}
		for (Thread th : this.workers) {
			th.start();
		}
	}

	public synchronized void stop() {
		for (DispatchingThread th : this.workers) {
			th.stopIt();
		}
		try {
			for (DispatchingThread th : this.workers) {
				th.join();
			}
		}
		catch (InterruptedException e) {
			LOGGER.warn("Stopping of mixer threads interrupted", e);
		}

		// cleanup for potential restart...
		this.workers.clear();
	}

	public int size() {
		int nb = 0;
		for (int i = 0 ; i < this.input.size() ; ++ i) {
			final DispatchingThread worker = this.workers.isEmpty() ? null : this.workers.get(i);
			nb += this.input.get(i).size() + (worker != null && worker.isAlive() && worker.hasElementInTransit() ? 1 : 0);
		}
		nb += this.output.size();
		return nb;
	}

	public int sizeInTransit() {
		int nb = 0;
		for (int i = 0 ; i < this.input.size() ; ++ i) {
			final DispatchingThread worker = this.workers.isEmpty() ? null : this.workers.get(i);
			nb += (worker != null && worker.isAlive() && worker.hasElementInTransit() ? 1 : 0);
		}
		return nb;
	}

	/**
	 * @param element some element which just made it from one of the input queue to the output queue
	 * @param fromQueue the input queue the element originally came from
	 * @throws IllegalArgumentException if the given input queue is not one of the original input queues
	 */
	protected void onElementOffered(T element, BlockingQueue<T> fromQueue) {
	}

	/**
	 * Thread de dispatching d'une queue d'entrée vers la queue de sortie
	 */
	private class DispatchingThread extends BlockingQueuePollingThread<T> {

		private final BlockingQueue<T> output;
		private volatile T transitting;
		private boolean offered;

		private DispatchingThread(String name, @NotNull BlockingQueue<T> input, @NotNull BlockingQueue<T> output) {
			super(name, POLL_TIMEOUT, input);
			this.output = output;
		}

		@Override
		protected void processElement(@NotNull T element) throws InterruptedException {
			offered = false;
			transitting = element;
			while (!shouldStop()) {
				if (output.offer(transitting, OFFER_TIMEOUT.toNanos(), TimeUnit.NANOSECONDS)) {
					transitting = null;
					offered = true;
					break;
				}
			}
		}

		@Override
		protected void onElementProcessed(@NotNull T element, @Nullable Throwable t) {
			super.onElementProcessed(element, t);
			if (offered) {
				notifyElementOffered(element);
			}
		}

		@Override
		protected void onStop() {
			super.onStop();
			if (transitting != null) {
				LOGGER.warn("Element " + transitting + " was taken from the input queue and never made it to the output queue");
			}
		}

		private void notifyElementOffered(T newlyOffered) {
			try {
				onElementOffered(newlyOffered, getPolledQueue());
			}
			catch (Throwable t) {
				LOGGER.warn("Exception raised while notifing offered element " + newlyOffered, t);
			}
		}

		public boolean hasElementInTransit() {
			return transitting != null;
		}
	}
}
