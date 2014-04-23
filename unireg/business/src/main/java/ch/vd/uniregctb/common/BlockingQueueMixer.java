package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Active object which takes elements from several input queues and transfers them into a single ouput queue
 * @param <T> type of the managed elements
 */
public class BlockingQueueMixer<T> {

	private static final int POLL_TIMEOUT = 100;    // ms
	private static final int OFFER_TIMEOUT = 100;    // ms

	private static final Logger LOGGER = Logger.getLogger(BlockingQueueMixer.class);

	private final List<BlockingQueue<T>> input;
	private final BlockingQueue<T> output;
	private final List<WorkingThread> workers;

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
			this.workers.add(new WorkingThread(threadNameGenerator.getNewThreadName(), in, this.output));
		}
		for (Thread th : this.workers) {
			th.start();
		}
	}

	public synchronized void stop() {
		for (WorkingThread th : this.workers) {
			th.stopIt();
		}
		try {
			for (WorkingThread th : this.workers) {
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
			final WorkingThread worker = this.workers.isEmpty() ? null : this.workers.get(i);
			nb += this.input.get(i).size() + (worker != null && worker.isAlive() && worker.hasElementInTransit() ? 1 : 0);
		}
		nb += this.output.size();
		return nb;
	}

	public int sizeInTransit() {
		int nb = 0;
		for (int i = 0 ; i < this.input.size() ; ++ i) {
			final WorkingThread worker = this.workers.isEmpty() ? null : this.workers.get(i);
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
	 * Dispatching thread
	 */
	private class WorkingThread extends Thread {

		private final BlockingQueue<T> input;
		private final BlockingQueue<T> output;
		private volatile boolean stopping = false;
		private volatile T transitting;

		private WorkingThread(String name, BlockingQueue<T> input, BlockingQueue<T> output) {
			super(name);
			this.input = input;
			this.output = output;
		}

		@Override
		public void run() {
			LOGGER.info("Starting queue mixer thread " + getName());
			try {
				while (!stopping) {
					transitting = input.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
					if (transitting != null) {
						while (!stopping) {
							if (output.offer(transitting, OFFER_TIMEOUT, TimeUnit.MILLISECONDS)) {
								final T newlyOffered = transitting;
								transitting = null;
								notifyElementOffered(newlyOffered);
								break;
							}
						}
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.warn("Queue mixer thread " + getName() + " interrupted", e);
			}
			catch (RuntimeException e) {
				LOGGER.warn("Queue mixer thread " + getName() + " will be stopped due to raised exception", e);
			}
			finally {
				LOGGER.info("Stopping queue mixer thread " + getName());
				if (transitting != null) {
					LOGGER.warn("Element " + transitting + " was taken from the input queue and never made it to the output queue");
				}
			}
		}

		private void notifyElementOffered(T newlyOffered) {
			try {
				onElementOffered(newlyOffered, input);
			}
			catch (Throwable t) {
				LOGGER.warn("Exception raised while notifing offered element " + newlyOffered, t);
			}
		}

		public boolean hasElementInTransit() {
			return transitting != null;
		}

		public void stopIt() {
			stopping = true;
		}
	}
}
