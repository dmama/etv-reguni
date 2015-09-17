package ch.vd.uniregctb.indexer.async;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.uniregctb.worker.DeadThreadException;
import ch.vd.uniregctb.worker.WorkingQueue;

/**
 * Tiers indexer utilisé pour l'indexation ou fil-de-l'eau des tiers de la base de données.
 */
public class OnTheFlyTiersIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OnTheFlyTiersIndexer.class);

	private static final int MIN_THREADS = 1; // nombre minimal de threads d'indexation
	private static final int MAX_THREADS = 4; // nombre maximal de threads d'indexation

	private static final long WATCH_PERIODE = 10000L; // 10 secondes
	private static final long LOW_LEVEL = 100L; // niveau minimal de la queue avant diminution du nombre de threads
	private static final long HIGH_LEVEL = 1000L; // niveau maximal de la queue avant augmentation du nombre de threads

	private final GlobalTiersIndexerImpl indexer;
	private final PlatformTransactionManager transactionManager;
	private final SessionFactory sessionFactory;
	private final Dialect dialect;

	private final WorkingQueue<Long> queue;

	private final Timer timer = new Timer();

	public OnTheFlyTiersIndexer(GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, Dialect dialect) {

		this.indexer = indexer;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;
		this.dialect = dialect;

		// Un queue bloquante de longueur illimitée
		this.queue = new WorkingQueue<>(MIN_THREADS, new TiersIndexerWorker(null, indexer, sessionFactory, transactionManager, dialect, "OnTheFly", null));
		this.queue.start();

		// Démarre le monitoring de la queue
		final TimerTask monitoring = new TimerTask() {
			@Override
			public void run() {
				monitor();
			}
		};
		this.timer.schedule(monitoring, WATCH_PERIODE, WATCH_PERIODE);
	}

	/**
	 * Demande l'indexation ou la ré-indexation d'un tiers.
	 * <p/>
	 * Cette méthode retourne immédiatement : l'indexation proprement dites est déléguée à un thread asynchrone.
	 *
	 * @param id l'id du tiers à indexer
	 */
	public void schedule(Long id) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Ajout de l'id [" + id + "] dans la queue...");
			}
			queue.put(id);
		}
		catch (InterruptedException e) {
			throw new IndexerException(e);
		}
	}

	public void schedule(Collection<Long> ids) {
		try {
			for (Long id : ids) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Ajout de l'id [" + id + "] dans la queue...");
				}
				queue.put(id);
			}
		}
		catch (InterruptedException e) {
			throw new IndexerException(e);
		}
	}

	/**
	 * Attends que tous les tiers dont l'indexation a été demandée aient été indexés. Cette méthode bloque donc tant que la queue d'indexation est pleine.
	 * <p/>
	 * <b>Note:</b> cette méthode n'empêche pas d'autres threads de scheduler l'indexation de nouveau tiers. En d'autres termes, le temps d'attente peu s'allonger indéfiniment.
	 */
	public void sync() {
		LOGGER.trace("Sync de la queue...");
		try {
			queue.sync();
		}
		catch (DeadThreadException e) {
			throw new IndexerException(e);
		}
		LOGGER.trace("Terminé.");
	}

	/**
	 * Vide la queue d'indexation et annule tous les indexations en cours
	 */
	public void reset() {
		LOGGER.trace("Clear de la queue...");
		queue.reset();
		LOGGER.trace("Terminé.");
	}

	/**
	 * Relâche les ressources et arrête complétement l'indexer
	 */
	public void destroy() {

		// Arrête le monitoring de la queue
		timer.cancel();

		// Vide la queue et arrête les threads
		reset();
		queue.shutdown();
	}

	/**
	 * Monitor la queue et adapte le nombre de threads en fonction de la demande.
	 */
	private void monitor() {

		synchronized (queue) {

			queue.purgeDeadWorkers();

			final int queueSize = queue.size();
			final int threadSize = queue.workersCount();

			if (queueSize < LOW_LEVEL && threadSize > MIN_THREADS) {
				// on enlève un thread
				final int last = threadSize - 1;
				try {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Demande de suppression du dernier thread d'indexation...");
					}
					final String name = queue.removeLastWorker();
					LOGGER.info("Supprimé le thread d'indexation " + name + " (threadSize=" + (threadSize - 1) + ", queueSize=" + queueSize + ')');
				}
				catch (DeadThreadException e) {
					LOGGER.warn(String.format("Le thread d'indexation %s était déjà mort", e.getThreadName()));
				}
			}

			if (queueSize > HIGH_LEVEL && threadSize < MAX_THREADS) {
				// on ajoute un thread
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Demande d'ajout d'un nouveau thread d'indexation...");
				}
				final String name = queue.addNewWorker(new TiersIndexerWorker(null, indexer, sessionFactory, transactionManager, dialect, "OnTheFly", null));
				LOGGER.info("Ajouté un thread d'indexation " + name + " (threadSize=" + (threadSize + 1) + ", queueSize=" + queueSize + ')');
			}
		}
	}

	public int getQueueSize() {
		return queue.size();
	}

	public int getActiveThreadNumber() {
		return queue.workersCount();
	}
}
