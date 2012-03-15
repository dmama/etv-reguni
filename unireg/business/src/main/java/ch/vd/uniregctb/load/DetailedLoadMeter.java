package ch.vd.uniregctb.load;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DetailedLoadMeter<T> implements DetailedLoadMonitorable {

	/**
	 * Nombre d'appels actuellement en cours
	 */
	private final AtomicInteger currentLoad = new AtomicInteger(0);

	/**
	 * Simple pointeur vers des données de charge courante
	 */
	private static final class DetailHolder {
		public LoadDetail detail;
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
	 */
	private final List<DetailHolder> detailHolders = new LinkedList<DetailHolder>();

	/**
	 * Convertisseur en chaîne de caractères
	 */
	private final LoadDetailRenderer<T> renderer;
	
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
				detailHolders.add(holder);
			}
			finally {
				lock.unlock();
			}
			return holder;
		}
	};

	private static final LoadDetailRenderer DEFAULT_RENDERER = new LoadDetailRenderer<Object>() {
		@Override
		public String toString(Object object) {
			return object != null ? object.toString() : null;
		}
	};

	public DetailedLoadMeter() {
		//noinspection unchecked
		this(DEFAULT_RENDERER);
	}
	
	public DetailedLoadMeter(LoadDetailRenderer<T> renderer) {
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
		details.get().detail = new LoadDetailImpl<T>(desc, ts, renderer);
		return ts;
}

	/**
	 * Doit être appelé lorsque le traitement relatif à l'appel précédemment enregistré par un appel à {@link #start} est terminé
	 * @return {@link System#nanoTime() timestamp} à la fin de l'appel
	 */
	public long end() {
		details.get().detail = null;
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
			d = new LinkedList<LoadDetail>();
			for (DetailHolder dh : detailHolders) {
				if (dh.detail != null) {
					d.add(dh.detail);
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
