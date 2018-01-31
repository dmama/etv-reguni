package ch.vd.uniregctb.indexer.async;

import java.time.Duration;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.uniregctb.worker.BatchWorker;
import ch.vd.uniregctb.worker.DeadThreadException;
import ch.vd.uniregctb.worker.WorkingQueue;

/**
 * Tiers indexer utilisé lors de l'indexation ou la re-indexation en masse des tiers de la base de données.
 */
public class MassTiersIndexer {

	private boolean isInit = false;
	private boolean enabled = true;

	private final WorkingQueue<Long> queue;

	private long totalCpuTime;
	private long totalUserTime;
	private long totalExecTime;

	public MassTiersIndexer(GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, int nbThreads, int queueByThreadSize, @NotNull Mode mode,
	                        Dialect dialect, @Nullable ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this(nbThreads, queueByThreadSize,
		     new TiersIndexerWorker(false,                                                          // on indexe une liste précise de tiers, par besoin d'étendre l'indexation à d'autres tiers.
		                            (mode == Mode.DIRTY_ONLY || mode == Mode.FULL_INCREMENTAL),     // on n'enlève pas préalablement les données indexées en mode FULL et MISSING_ONLY,
		                                                                                            // parce que - par définition - ces données n'existent pas dans ces modes-là.
		                            indexer,
		                            sessionFactory,
		                            transactionManager,
		                            dialect,
		                            "Mass",
		                            serviceCivilCacheWarmer));
	}

	// pour le testing uniquement
	public MassTiersIndexer(int nbThreads, int queueByThreadSize, BatchWorker<Long> worker) {

		// Le flag doit etre setté avant le démarrage des threads sinon elles se terminent tout de suite...
		isInit = true;

		queue = new WorkingQueue<>(queueByThreadSize * nbThreads, nbThreads, worker);
		queue.start();
	}

	public void clearQueue() {
		Assert.isTrue(isInit);
		queue.reset();
	}

	/**
	 * Attend que les threads d'indexation soient terminés après avoir terminé le travail (= vidé la queue d'indexation)
	 *
	 * @return <code>true</code> si tous les threads étaient déjà morts avant même qu'on leur demande de s'arrêter, <code>false</code> si au moins un tournait encore (ou que rien n'avait encore été
	 *         lancé)
	 */
	public boolean terminate() {

		if (!isInit) {
			return false;
		}

		isInit = false;

		final WorkingQueue.WorkStats stats = queue.shutdown();

		totalCpuTime += stats.getCpuTime();
		totalUserTime += stats.getUserTime();
		totalExecTime += stats.getExecTime();

		return stats.getDeadThreadsCount() == stats.getThreadsCount();
	}

	/**
	 * @return temps total cpu en nanosecondes
	 */
	public long getTotalCpuTime() {
		return totalCpuTime;
	}

	/**
	 * @return temps total user en nanosecondes
	 */
	public long getTotalUserTime() {
		return totalUserTime;
	}

	/**
	 * @return temps total d'exécution (wallclock) en nanosecondes
	 */
	public long getTotalExecTime() {
		return totalExecTime;
	}

	public void queueTiersForIndexation(Long id) throws Exception {
		Assert.isTrue(isInit);
		Assert.isTrue(enabled, "L'ASYNC indexer est disabled, ne devrait pas etre appelé!");
		queue.put(id);
	}

	/**
	 * Insère le tiers spécifié dans la queue, en attendant le temps nécessaire pour qu'une place se libère dans la queue.
	 *
	 * @param id      l'id du tiers à réindexer
	 * @param timeout le temps maximum d'attente avant d'abandonner l'insertion
	 * @return <tt>true</tt> en cas de succès, ou <tt>false</tt> si le temps maximum d'attente est dépassé avant qu'une place se libère dans la queue.
	 * @throws Exception s'il n'est pas possible d'accepter l'id
	 */
	public boolean offerTiersForIndexation(Long id, Duration timeout) throws Exception {
		Assert.isTrue(isInit);
		Assert.isTrue(enabled, "L'ASYNC indexer est disabled, ne devrait pas etre appelé!");
		return queue.offer(id, timeout);
	}

	/**
	 * @return <code>true</code> si au moins un des threads lancés est toujours vivant
	 */
	public boolean isAlive() {
		return queue.anyWorkerAlive();
	}

	/**
	 * Cette méthode attends que la queue des tiers soit vide et retourne. Elle permet de s'assurer que tous les tiers enqueués <b>avant</b> l'appel à la méthode ont bien été indéxés lorsque la méthode
	 * retourne.
	 *
	 * @throws DeadThreadException si tous les workers sont morts, ou qu'il n'y pas de workers définis.
	 */
	public void sync() throws DeadThreadException {
		queue.sync();
	}

	public void setEnabled(boolean e) {
		enabled = e;
	}
}
