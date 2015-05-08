package ch.vd.uniregctb.migration.pm.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Classe d'accès à des sections critiques partagées (read) ou exclusives (write)
 */
public class LockHelper {

	private final ReentrantReadWriteLock rwlock;

	/**
	 * @param fair paramètre directement passé au constructeur du {@link ReentrantReadWriteLock}
	 */
	public LockHelper(boolean fair) {
		this.rwlock = new ReentrantReadWriteLock(fair);
	}

	/**
	 * Appelle le callback dans un contexte de verrou exclusif
	 * @param callback callback à appeler
	 * @param <T> type de la valeur retournée par le callback
	 * @return la valeur renvoyée par le callback
	 */
	public <T> T doInWriteLock(Supplier<T> callback) {
		return doInLock(rwlock.writeLock(), callback);
	}

	/**
	 * Appelle le callback dans un contexte de verrou exclusif
	 * @param callback callback à appeler
	 */
	public void doInWriteLock(Runnable callback) {
		doInWriteLock(() -> { callback.run(); return null; });
	}

	/**
	 * Appelle le callback dans un contexte de verrou partagé
	 * @param callback callback à appeler
	 * @param <T> type de la valeur retournée par le callback
	 * @return la valeur renvoyée par le callback
	 */
	public <T> T doInReadLock(Supplier<T> callback) {
		return doInLock(rwlock.readLock(), callback);
	}

	/**
	 * Appelle le callback dans un contexte de verrou partagé
	 * @param callback callback à appeler
	 */
	public void doInReadLock(Runnable callback) {
		doInReadLock(() -> { callback.run(); return null; });
	}

	private static <T> T doInLock(Lock lock, Supplier<T> callback) {
		lock.lock();
		try {
			return callback.get();
		}
		finally {
			lock.unlock();
		}
	}
}
