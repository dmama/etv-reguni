package ch.vd.uniregctb.load;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.MutableObject;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.uniregctb.common.LockHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.stats.DetailedLoadMonitorable;
import ch.vd.uniregctb.stats.LoadDetail;

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
	private final LockHelper lockHelper = new LockHelper();
	
	/**
	 * Ensemble de toutes les instances de {@link DetailHolder} créées par tous les threads. Son accès est protégé en R/W
	 * par le verrou géré dans l'instance {@link #lockHelper}.
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
	private final ThreadLocal<DetailHolder> details = ThreadLocal.withInitial(() -> {
		final DetailHolder holder = new DetailHolder();
		lockHelper.doInWriteLock(() -> detailHolders.put(holder, null));
		return holder;
	});

	public DetailedLoadMeter() {
		this(StringRenderer.DEFAULT);
	}
	
	public DetailedLoadMeter(StringRenderer<? super T> renderer) {
		this.renderer = renderer;
	}

	/**
	 * Doit être appelé au moment du démarrage d'un nouvel appel
	 * @param desc descripteur de l'appel (la méthode {@link #toString} sera appelée pour obtenir une description de l'appel)
	 * @return {@link Instant} courant au moment de l'appel
	 */
	public Instant start(T desc) {
		currentLoad.incrementAndGet();
		final Instant start = InstantHelper.get();
		details.get().setValue(new LoadDetailImpl<>(desc, start, Thread.currentThread().getName(), renderer));
		return start;
}

	/**
	 * Doit être appelé lorsque le traitement relatif à l'appel précédemment enregistré par un appel à {@link #start} est terminé
	 * @return {@link Instant} à la fin de l'appel
	 */
	public Instant end() {
		details.get().setValue(null);
		currentLoad.decrementAndGet();
		return InstantHelper.get();
	}
	
	private static long timestamp() {
		return System.nanoTime();
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return lockHelper.doInReadLock(() -> {
			final List<LoadDetail> d = new LinkedList<>();
			for (DetailHolder dh : detailHolders.keySet()) {
				if (dh != null && dh.getValue() != null) {
					d.add(dh.getValue());
				}
			}
			return d;
		});
	}

	@Override
	public int getLoad() {
		return currentLoad.intValue();
	}
}
