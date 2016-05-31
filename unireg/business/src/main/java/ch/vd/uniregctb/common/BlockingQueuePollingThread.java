package ch.vd.uniregctb.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public abstract class BlockingQueuePollingThread<T> extends PollingThread<T> {

	private final BlockingQueue<T> queue;

	/**
	 * Constructor
	 * @param name nom du thread
	 * @param queue queue à surveiller
	 */
	protected BlockingQueuePollingThread(String name, @NotNull BlockingQueue<T> queue) {
		super(name);
		this.queue = queue;
	}

	/**
	 * Constructor
	 * @param name nom du thread
	 * @param queue queue à surveiller
	 */
	protected BlockingQueuePollingThread(String name, long pollingTimeout, @NotNull TimeUnit pollingTimeoutUnit, @NotNull BlockingQueue<T> queue) {
		super(name, pollingTimeout, pollingTimeoutUnit);
		this.queue = queue;
	}

	/**
	 * Disponible pour les classes dérivées pour accéder à la queue surveillée
	 * @return la queue surveillée
	 */
	protected BlockingQueue<T> getPolledQueue() {
		return queue;
	}

	@Override
	protected final T poll(long timeout, @NotNull TimeUnit timeUnit) throws InterruptedException {
		return queue.poll(timeout, timeUnit);
	}
}
