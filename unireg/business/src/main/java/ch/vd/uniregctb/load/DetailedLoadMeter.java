package ch.vd.uniregctb.load;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.mutable.MutableObject;

import ch.vd.uniregctb.common.StringRenderer;

public class DetailedLoadMeter<T> implements DetailedLoadMonitorable {

	/**
	 * Nombre d'appels actuellement en cours
	 */
	private final AtomicInteger currentLoad = new AtomicInteger(0);

	/**
	 * Simple pointeur vers des données de charge courante, avec surcharge du {@link #hashCode()} et du {@link #equals(Object)}
	 * afin de se baser sur l'identité du container
	 */
	private static final class DetailHolder extends MutableObject<LoadDetail> {
		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}
	}

	/**
	 * RWLock pour protéger les accès à l'ensemble {@link #detailHolders}
	 */
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	/**
	 * Ensemble de toutes les instances de {@link DetailHolder} créées par tous les threads. Son accès est protégé en R/W
	 * par le verrou {@link #rwLock}.
	 * <ul>
	 *     <li>Accès <b>WRITE</b> nécessaire lors de l'ajout de nouveaux éléments dans la liste - une fois par thread dans le pool</li>
	 *     <li>Accès <b>READ</b> nécessaire lors de l'interrogation du contenu de la liste - voir méthode {@link #getLoadDetails()}</li>
	 * </ul>
	 * A part ces deux cas, les threads eux-mêmes n'ont pas besoin de synchronisation au moment des appels
	 * <p/>
	 * L'idée d'avoir une {@link WeakHashMap} ici est de se débarasser de l'entrée quand le thread se termine (cas des pools de theads dont
	 * la taille n'est pas nécessairement constante)
	 */
	private final Map<DetailHolder, ?> detailHolders = new WeakHashMap<>();

	/**
	 * Convertisseur en chaîne de caractères
	 */
	private final StringRenderer<? super T> renderer;
	
	/**
	 * Container du descripteur de la charge en cours : chacune des instances par thread ne nécessite aucune synchronisation
	 * particulière (c'est juste un pointeur = assignation atomique), la synchronisation est uniquement gérée au niveau de 
	 * la collection {@link #detailHolders}
	 */
	private final ThreadLocal<DetailHolder> details = new ThreadLocal<DetailHolder>() {
		@Override
		protected DetailHolder initialValue() {
			final DetailHolder holder = new DetailHolder();
			final Lock lock = rwLock.writeLock();
			lock.lock();
			try {
				detailHolders.put(holder, null);
			}
			finally {
				lock.unlock();
			}
			return holder;
		}
	};

	public DetailedLoadMeter() {
		this(StringRenderer.DEFAULT);
	}
	
	public DetailedLoadMeter(StringRenderer<? super T> renderer) {
		this.renderer = renderer;
	}

	/**
	 * Doit être appelé au moment du démarrage d'un nouvel appel
	 * @param desc descripteur de l'appel (la méthode {@link #toString} sera appelée pour obtenir une description de l'appel)
	 * @return {@link System#nanoTime() timestamp} du démarrage de l'appel 
	 */
	public long start(T desc) {
		currentLoad.incrementAndGet();
		final long ts = timestamp();
		details.get().setValue(new LoadDetailImpl<>(desc, ts, Thread.currentThread().getName(), renderer));
		return ts;
}

	/**
	 * Doit être appelé lorsque le traitement relatif à l'appel précédemment enregistré par un appel à {@link #start} est terminé
	 * @return {@link System#nanoTime() timestamp} à la fin de l'appel
	 */
	public long end() {
		details.get().setValue(null);
		currentLoad.decrementAndGet();
		return timestamp();
	}
	
	private static long timestamp() {
		return System.nanoTime();
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		final List<LoadDetail> d;
		
		final Lock lock = rwLock.readLock();
		lock.lock();
		try {
			d = new LinkedList<>();
			for (DetailHolder dh : detailHolders.keySet()) {
				if (dh != null && dh.getValue() != null) {
					d.add(dh.getValue());
				}
			}
		}
		finally {
			lock.unlock();
		}
		return d;
	}

	@Override
	public int getLoad() {
		return currentLoad.intValue();
	}
}
