package ch.vd.unireg.common;

import java.time.Duration;
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
	protected BlockingQueuePollingThread(String name, @NotNull Duration pollingTimeout, @NotNull BlockingQueue<T> queue) {
		super(name, pollingTimeout);
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
	protected final T poll(@NotNull Duration timeout) throws InterruptedException {
		return queue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
	}
}
