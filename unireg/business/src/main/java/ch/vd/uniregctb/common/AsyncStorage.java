package ch.vd.uniregctb.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;

/**
 * Classe de stockage avec accès multithread
 */
public class AsyncStorage<K, V> {

	private final AtomicInteger nbReceived = new AtomicInteger(0);

	/**
	 * Verrou qui permet d'accéder à la map
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * Condition levée quand tout le monde doit se réveiller parce qu'un nouveau document est arrivé
	 */
	private final Condition newDocument = lock.newCondition();

	/**
	 * Espace de stockage
	 */
	private final Map<K, DataHolder<V>> map = new HashMap<K, DataHolder<V>>();

	/**
	 * Classe abstraite de base des résultats renvoyés par la méthode {@link #get}
	 * @param <K> le type de la clé de stockage
	 */
	public static abstract class RetrievalResult<K> {
		public final K key;
    	public RetrievalResult(K key) {
			this.key = key;
		}
	}

	/**
	 * Valeur renvoyée par la méthode {@link #get} en cas de timeout
	 * @param <K> le type de la clé de stockage
	 */
	public static final class RetrievalTimeout<K> extends RetrievalResult<K> {
		public RetrievalTimeout(K key) {
			super(key);
		}
	}

	/**
	 * Valeur renvoyée par la méthode {@link #get} en cas de retour positif
	 * @param <K> le type de la clé de stockage
	 * @param <V> le type de la valeur stockée
	 */
	public static final class RetrievalData<K, V> extends RetrievalResult<K> {
		public final V data;
		public RetrievalData(K key, V data) {
			super(key);
			this.data = data;
		}
	}

	/**
	 * Ajoute un nouvel élément à l'espace de stockage et notifie tous les threads en attente
	 * qu'un nouvel élément est arrivé
	 * @param key clé de stockage
	 * @param value valeur à stocker
	 */
	public final void add(K key, @Nullable V value) {
		lock.lock();
		try {
			nbReceived.incrementAndGet();
			map.put(key, buildDataHolder(value));
			signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * A surcharger dans les classes dérivées si nécessaire
	 * @param value valeur à stocker
	 * @return une instance de DataHolder qui contient cette valeur
	 */
	protected DataHolder<V> buildDataHolder(@Nullable V value) {
		return new DataHolder<V>(value);
	}

	/**
	 * Attend le temps qu'il faut jusqu'à ce que le stockage ait effectivement un élément correspondant à la clé et le renvoie
	 * @param key clé recherchée
	 * @param timeout longueur du timeout (unité dans le champ unit) - un timeout de 0 signifie "on n'attend pas"
	 * @param unit unité du timeout
	 * @return <code>null</code> en cas de timeout, sinon, la valeur extraite de l'espace de stockage (duquel elle est enlevée)
	 * @throws InterruptedException en cas d'interruption du thread pendant l'attente
	 */
	@NotNull
	public final RetrievalResult<K> get(K key, long timeout, TimeUnit unit) throws InterruptedException {
		lock.lock();
		try {
			final long tsMaxAttente = System.nanoTime() + unit.toNanos(timeout);
			while (true) {
				final DataHolder<V> value = map.remove(key);
				if (value != null) {
					// fini -> on revient avec la valeur
					return new RetrievalData<K, V>(key, value.get());
				}

				final long tempsRestant = tsMaxAttente - System.nanoTime();
				if (tempsRestant <= 0) {
					// fini -> le timeout a sonné !
					return new RetrievalTimeout<K>(key);
				}

				awaitNanos(tempsRestant);
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * @return Le nombre d'éléments entrés dans l'espace de stockage
	 */
	public final int getNbReceived() {
		return nbReceived.intValue();
	}

	/**
	 * @return Le nombre d'éléments actuellement présents dans l'espace de stockage
	 */
	public final int size() {
		lock.lock();
		try {
			return map.size();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Interface du callback lors d'un appel à {@link AsyncStorage#doInLockedEnvironment(ch.vd.uniregctb.common.AsyncStorage.Action)}
	 * @param <K> type de la clé de stockage
	 * @param <V> type de la valeur de stockage
	 * @param <T> type de la valeur retournée par l'action
	 */
	protected static interface Action<K, V, T> {

		/**
		 * Corps de l'action
		 * @param entries Collection des éléments dans l'espace de stockage
		 * @return Ce que l'action veut retourner (et qui servira de valeur de retour de l'appel à {@link AsyncStorage#doInLockedEnvironment(ch.vd.uniregctb.common.AsyncStorage.Action)}
		 */
		T execute(Iterable<Map.Entry<K, DataHolder<V>>> entries);
	}

	/**
	 * Appelé pour faire une action alors que l'ensemble de l'espace de stockage ne peut pas être modifié (= environnement protégé)
	 * @param action Callback pour l'action elle-même
	 * @param <T> Type de retour de l'action
	 * @return La valeur retournée par l'exécution de l'action
	 */
	protected final <T> T doInLockedEnvironment(Action<K, V, T> action) {
		lock.lock();
		try {
			return action.execute(map.entrySet());
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Appelable depuis l'intérieur d'un "environnement protégé" pour attendre la levée du signal de réveil
	 * @throws InterruptedException En cas d'interruption brutale de l'attente
	 * @see #signalAll()
	 */
	protected final void await() throws InterruptedException {
		Assert.isTrue(lock.isHeldByCurrentThread());
		newDocument.await();
	}

	/**
	 * Appelable depuis l'intérieur d'un "environnement protégé" pour attendre la levée du signal de réveil, mais pas plus longtemps que le timeout indiqué (en nanosecondes)
	 * @throws InterruptedException En cas d'interruption brutale de l'attente
	 * @see #signalAll()
	 */
	protected final void awaitNanos(long nanos) throws InterruptedException {
		Assert.isTrue(lock.isHeldByCurrentThread());
		newDocument.awaitNanos(nanos);
	}

	/**
	 * Appelable depuis l'intérieur d'un "environnement protégé" pour lancer le signal de réveil des threads éventuellement en attente
	 */
	protected final void signalAll() {
		Assert.isTrue(lock.isHeldByCurrentThread());
		newDocument.signalAll();
	}
}
