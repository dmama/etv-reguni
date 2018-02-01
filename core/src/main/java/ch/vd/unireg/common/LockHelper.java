package ch.vd.unireg.common;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Petite classe utilitaire qui permet de jouer facilement avec les verrous read/write
 */
public final class LockHelper {

	private final ReadWriteLock lock;

	public LockHelper() {
		this.lock = new ReentrantReadWriteLock();
	}

	public LockHelper(boolean fair) {
		this.lock = new ReentrantReadWriteLock(fair);
	}

	/**
	 * Exécute l'action dans le contexte du lock "read"
	 * @param action action à exécuter
	 * @param <T> type de la valeur de retour
	 * @return valeur retournée par l'action
	 */
	public <T> T doInReadLock(Supplier<T> action) {
		return doInLock(lock.readLock(), action);
	}

	/**
	 * Exécute l'action dans le contexte du lock "write"
	 * @param action action à exécuter
	 * @param <T> type de la valeur de retour
	 * @return valeur retournée par l'action
	 */
	public <T> T doInWriteLock(Supplier<T> action) {
		return doInLock(lock.writeLock(), action);
	}

	/**
	 * Exécute l'action dans le contexte du lock "read"
	 * @param action action à exécuter
	 */
	public void doInReadLock(Runnable action) {
		doInReadLock(buildSupplierFromRunnable(action));
	}

	/**
	 * Exécute l'action dans le contexte du lock "write"
	 * @param action action à exécuter
	 */
	public void doInWriteLock(Runnable action) {
		doInWriteLock(buildSupplierFromRunnable(action));
	}

	/**
	 * Constitue un {@link Supplier} à partir d'un {@link Runnable}
	 * @param action {@link Runnable} à encapsuler
	 * @return le {@link Supplier} qui va bien
	 */
	private static Supplier<?> buildSupplierFromRunnable(Runnable action) {
		return () -> {
			action.run();
			return null;
		};
	}

	/**
	 * Exécute l'action dans le contexte du verrou passé en paramètre
	 * @param lock le verrou à activer
	 * @param action l'action à exécuter
	 * @param <T> le type de la valeur retournée par l'action
	 * @return la valeur retournée par l'action
	 */
	private static <T> T doInLock(Lock lock, Supplier<T> action) {
		lock.lock();
		try {
			return action.get();
		}
		finally {
			lock.unlock();
		}
	}
}
