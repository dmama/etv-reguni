package ch.vd.uniregctb.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de stockage avec accès multithread
 */
public class AsyncStorage<K, V> {

	private final AtomicInteger nbReceived = new AtomicInteger(0);

	/**
	 * Espace de stockage
	 */
	protected final Map<K, DataHolder<V>> map = new HashMap<K, DataHolder<V>>();

	protected static class DataHolder<V> {
		public final V data;
		protected DataHolder(@Nullable V data) {
			this.data = data;
		}
	}

	public static abstract class RetrievalResult<K> {
		public final K key;
    	public RetrievalResult(K key) {
			this.key = key;
		}
	}

	public static final class RetrievalTimeout<K> extends RetrievalResult<K> {
		public RetrievalTimeout(K key) {
			super(key);
		}
	}

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
	public void add(K key, @Nullable V value) {
		synchronized (map) {
			nbReceived.incrementAndGet();
			map.put(key, buildDataHolder(value));
			map.notifyAll();
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
	public RetrievalResult<K> get(K key, long timeout, TimeUnit unit) throws InterruptedException {
		synchronized (map) {
			final long tsMaxAttente = System.nanoTime() + unit.toNanos(timeout);
			while (true) {
				final DataHolder<V> value = map.remove(key);
				if (value != null) {
					// fini -> on revient avec la valeur
					return new RetrievalData<K, V>(key, value.data);
				}

				final long tempsRestant = tsMaxAttente - System.nanoTime();
				if (tempsRestant <= 0) {
					// fini -> le timeout a sonné !
					return new RetrievalTimeout<K>(key);
				}

				final long millis = TimeUnit.NANOSECONDS.toMillis(tempsRestant);
				final int nanos = (int) (tempsRestant - TimeUnit.MILLISECONDS.toNanos(millis));
				map.wait(millis, nanos);
			}
		}
	}

	/**
	 * @return Le nombre d'éléments entrés dans l'espace de stockage
	 */
	public int getNbReceived() {
		return nbReceived.intValue();
	}

	/**
	 * @return Le nombre d'éléments actuellement présents dans l'espace de stockage
	 */
	public int size() {
		synchronized (map) {
			return map.size();
		}
	}
}
