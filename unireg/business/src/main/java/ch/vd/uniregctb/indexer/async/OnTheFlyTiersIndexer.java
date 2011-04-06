package ch.vd.uniregctb.indexer.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;

/**
 * Tiers indexer utilisé pour l'indexation ou fil-de-l'eau des tiers de la base de données.
 */
public class OnTheFlyTiersIndexer {

	private static final Logger LOGGER = Logger.getLogger(OnTheFlyTiersIndexer.class);

	private static final int MIN_THREADS = 1; // nombre minimal de threads d'indexation
	private static final int MAX_THREADS = 4; // nombre maximal de threads d'indexation

	private static final long WATCH_PERIODE = 10000L; // 10 secondes
	private static final long LOW_LEVEL = 100L; // niveau minimal de la queue avant diminution du nombre de threads
	private static final long HIGH_LEVEL = 1000L; // niveau maximal de la queue avant augmentation du nombre de threads

	private final GlobalTiersIndexerImpl indexer;
	private final PlatformTransactionManager transactionManager;
	private final SessionFactory sessionFactory;
	private final Dialect dialect;

	private int count = 0;
	private final ArrayList<AsyncTiersIndexerThread> threads = new ArrayList<AsyncTiersIndexerThread>();
	private BlockingQueue<Long> queue;

	private final Timer timer = new Timer();
	private final TimerTask monitoring = new TimerTask() {
		@Override
		public void run() {
			monitor();
		}
	};

	public OnTheFlyTiersIndexer(GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, Dialect dialect) {

		this.indexer = indexer;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;
		this.dialect = dialect;

		// Un queue bloquante de longueur illimitée
		this.queue = new LinkedBlockingQueue<Long>();

		// Crée le nombre minimal de threads
		this.count = 0;
		for (; count < MIN_THREADS; count++) {
			final AsyncTiersIndexerThread t = new AsyncTiersIndexerThread(queue, null, indexer, sessionFactory, transactionManager, dialect);
			t.setName("OnTheFly-" + count);
			t.start();
			this.threads.add(t);
		}

		// Démarre le monitoring de la queue
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
	 * <p/>
	 * <b>Note:</b> cette méthode n'empêche pas d'autres threads de scheduler l'indexation de nouveau tiers. En d'autres termes, le temps d'attente peu s'allonger indéfiniment.
	 */
	public void sync() {
		try {
			LOGGER.trace("Vidange de la queue...");
			while (!queue.isEmpty()) {
				Thread.sleep(10);
			}
		}
		catch (InterruptedException e) {
			throw new IndexerException(e);
		}

		LOGGER.trace("Sync des threads...");
		synchronized (threads) {
			for (AsyncTiersIndexerThread t : threads) {
				t.sync();
			}
		}
		LOGGER.trace("Terminé.");
	}

	/**
	 * Vide la queue d'indexation et annule tous les indexations en cours
	 */
	public void reset() {
		LOGGER.trace("Clear de la queue...");
		queue.clear();

		LOGGER.trace("Reset des threads...");
		synchronized (threads) {
			for (AsyncTiersIndexerThread t : threads) {
				t.reset();
			}
		}
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
		synchronized (threads) {
			for (AsyncTiersIndexerThread t : threads) {
				t.shutdown();
			}
			threads.clear();
		}
	}

	/**
	 * Monitor la queue et adapte le nombre de threads en fonction de la demande.
	 */
	private void monitor() {

		synchronized (threads) {
			purgeDeadThreads();

			final int queueSize = queue.size();
			final int threadSize = threads.size();

			if (queueSize < LOW_LEVEL && threadSize > MIN_THREADS) {
				// on enlève un thread
				final int last = threadSize - 1;
				final AsyncTiersIndexerThread t = threads.remove(last);
				LOGGER.info("Suppression d'un thread d'indexation " + t.getName() + " (threadSize=" + (threadSize - 1) + ", queueSize=" + queueSize + ")");
				t.shutdown();
			}

			if (queueSize > HIGH_LEVEL && threadSize < MAX_THREADS) {
				// on ajoute un thread
				final AsyncTiersIndexerThread t = new AsyncTiersIndexerThread(queue, null, indexer, sessionFactory, transactionManager, dialect);
				t.setName("OnTheFly-" + (count++));

				LOGGER.info("Ajout d'un thread d'indexation " + t.getName() + " (threadSize=" + (threadSize + 1) + ", queueSize=" + queueSize + ")");
				t.start();
				threads.add(t);
			}
		}
	}

	private void purgeDeadThreads() {
		for (int i = threads.size() - 1; i >= 0; i--) {
			final AsyncTiersIndexerThread t = threads.get(i);
			if (!t.isAlive()) {
				LOGGER.warn("Détecté un thread d'indexation mort : il sera redémarré si nécessaire");
				threads.remove(i);
			}
		}
	}
}
