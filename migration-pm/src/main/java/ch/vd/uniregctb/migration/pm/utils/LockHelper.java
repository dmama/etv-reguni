package ch.vd.uniregctb.migration.pm.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

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
		doInWriteLock(nullSupplierFromRunnable(callback));
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
		doInReadLock(nullSupplierFromRunnable(callback));
	}

	/**
	 * Converti une instance de {@link Runnable} en une instance de {@link Supplier} qui renvoie <code>null</code>.
	 * @param runnable le {@link Runnable} à convertir
	 * @param <T> un type arbitraire (<code>null</code> est de tout type)
	 * @return le {@link Supplier} adéquat
	 */
	@NotNull
	private static <T> Supplier<T> nullSupplierFromRunnable(Runnable runnable) {
		return () -> {
			runnable.run();
			return null;
		};
	}

	/**
	 * Execute le callback fourni dans le contexte du verrou fourni
	 * @param lock verrou à activer pendant l'exécution du callback
	 * @param callback callback à exécuter
	 * @param <T> type de la valeur retournée par le callback
	 * @return valeur retournée par le callback
	 */
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
