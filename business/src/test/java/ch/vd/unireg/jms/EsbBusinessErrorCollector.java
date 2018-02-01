package ch.vd.unireg.jms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.Nullable;

import ch.vd.technical.esb.EsbMessage;

public class EsbBusinessErrorCollector implements EsbBusinessErrorHandler {

	private final Lock lock = new ReentrantLock();
	private final Condition newElementCondition = lock.newCondition();
	private final List<EsbMessage> collectedItems = new LinkedList<>();

	@Override
	public void onBusinessError(EsbMessage esbMessage, String errorDescription, @Nullable Throwable throwable, EsbBusinessCode errorCode) throws Exception {
		final EsbMessage err = EsbBusinessErrorHandlerImpl.buildEsbErrorMessage(esbMessage, errorDescription, throwable, errorCode);
		lock.lock();
		try {
			collectedItems.add(err);
			newElementCondition.signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * @param sizeToWaitFor le nombre d'éléments collectés attendus
	 * @param timeout le temps maximal d'attente en millisecondes
	 * @return la liste si la taille a été atteinte dans le temps imparti, sinon null
	 * @throws InterruptedException en cas d'interruption du thread pendant l'attente
	 */
	public List<EsbMessage> waitForIncomingMessages(int sizeToWaitFor, long timeout) throws InterruptedException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout should be positive");
		}

		final long start = System.nanoTime();
		lock.lock();
		try {
			while (collectedItems.size() < sizeToWaitFor) {
				newElementCondition.await(timeout, TimeUnit.MILLISECONDS);
				if (collectedItems.size() < sizeToWaitFor) {
					final long now = System.nanoTime();
					final long remaining = timeout - TimeUnit.MICROSECONDS.toMillis(now - start);
					if (remaining <= 0) {
						return null;
					}
					timeout = remaining;
				}
			}
			return new ArrayList<>(collectedItems);
		}
		finally {
			lock.unlock();
		}
	}

	public void clear() {
		lock.lock();
		try {
			collectedItems.clear();
		}
		finally {
			lock.unlock();
		}
	}
}
