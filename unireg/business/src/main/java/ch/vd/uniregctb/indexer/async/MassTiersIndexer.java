package ch.vd.uniregctb.indexer.async;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;

/**
 * Tiers indexer utilisé lors de l'indexation ou la re-indexation en masse des tiers de la base de données.
 */
public class MassTiersIndexer {

	private static final Logger LOGGER = Logger.getLogger(MassTiersIndexer.class);

	private boolean isInit = false;
	private boolean enabled = true;

	private final List<AsyncTiersIndexerThread> threads;
	private final BlockingQueue<Long> queue;

	private long totalCpuTime;
	private long totalUserTime;
	private long totalExecTime;

	public MassTiersIndexer(GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, int nbThreads, int queueByThreadSize, Mode mode,
	                        Dialect dialect) {

		// Le flag doit etre setté avant le démarrage des threads sinon elles se terminent tout de suite...
		isInit = true;

		threads = new ArrayList<AsyncTiersIndexerThread>(nbThreads);
		queue = new ArrayBlockingQueue<Long>(queueByThreadSize * nbThreads);

		for (int i = 0; i < nbThreads; i++) {
			final AsyncTiersIndexerThread t = createIndexerThread(queue, indexer, transactionManager, sessionFactory, mode, dialect);
			threads.add(t);
			t.setName("Mass-" + i);
			LOGGER.info("Démarrage du thread " + t.getName());
			t.start();
		}
	}

	/**
	 * Surchargeable par les tests pour créer un thread custom et ainsi simuler des comportements particuliers
	 * @param queue queue de réception des identifiants des tiers à indexer
	 * @param indexer moteur d'indexation
	 * @param transactionManager gestionnaire de transactions
	 * @param sessionFactory session factory hibernate
	 * @param mode mode d'indexation
	 * @param dialect dialecte de la base de données
	 * @return un thread pour l'indexation de masse
	 */
	protected AsyncTiersIndexerThread createIndexerThread(BlockingQueue<Long> queue, GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory,
	                                                      Mode mode, Dialect dialect) {
		return new AsyncTiersIndexerThread(queue, mode, indexer, sessionFactory, transactionManager, dialect);
	}

	public void clearQueue() {
		Assert.isTrue(isInit);
		queue.clear();
	}

	/**
	 * Attend que les threads d'indexation soient terminés après avoir terminé le travail (= vidé la queue d'indexation)
	 * @return <code>true</code> si tous les threads étaient déjà morts avant même qu'on leur demande de s'arrêter, <code>false</code> si au moins un tournait encore (ou que rien n'avait encore été lancé)
	 */
	public boolean terminate() {

		if (!isInit) {
			return false;
		}

		isInit = false;

		int nbDeadThreads = 0;
		for (AsyncTiersIndexerThread thread : threads) {
			try {
				thread.shutdown();
			}
			catch (AsyncTiersIndexerThread.DeadThreadException e) {
				++ nbDeadThreads;
			}
		}
		final boolean allDeadBeforeJoiningAttempt = (nbDeadThreads > 0 && nbDeadThreads == threads.size());

		final ThreadMXBean mXBean = ManagementFactory.getThreadMXBean();
		for (AsyncTiersIndexerThread thread : threads) {
			final long cpuTime = mXBean.getThreadCpuTime(thread.getId());
			final long userTime = mXBean.getThreadUserTime(thread.getId());

			try {
				thread.join(10000);
			}
			catch (InterruptedException e) {
				// attente interrompue... pas grave, on aura attendu moins longtemps, c'est tout!
			}
			if (thread.isAlive()) {
				LOGGER.warn("Interruption forcée du thread " + thread.getName() + " qui ne s'est pas arrêté après 10 secondes d'attente.");
				thread.interrupt();
			}

			final long execTime = thread.getExecutionTime();
			totalCpuTime += cpuTime;
			totalUserTime += userTime;
			totalExecTime += execTime;
		}

		threads.clear();
		queue.clear();

		return allDeadBeforeJoiningAttempt;
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
	 * @param id
	 *            l'id du tiers à réindexer
	 * @param timeout
	 *            le temps maximum d'attente avant d'abandonner l'insertion, en unités de <tt>unit</tt>
	 * @param unit
	 *            une <tt>TimeUnit</tt> qui détermine comment intérpréter le paramètre <tt>timeout</tt>
	 * @return <tt>true</tt> en cas de succès, ou <tt>false</tt> si le temps maximum d'attente est dépassé avant qu'une place se libère dans
	 *         la queue.
     * @throws Exception s'il n'est pas possible d'accepter l'id
	 */
	public boolean offerTiersForIndexation(Long id, long timeout, TimeUnit unit) throws Exception {
		Assert.isTrue(isInit);
		Assert.isTrue(enabled, "L'ASYNC indexer est disabled, ne devrait pas etre appelé!");
		return queue.offer(id, timeout, unit);
	}

	/**
	 * @return <code>true</code> si au moins un des threads lancés est toujours vivant
	 */
	public boolean isAlive() {
		boolean alive = false;
		for (Thread t : threads) {
			alive |= (t != null && t.isAlive());
		}
		return alive;
	}

	/**
	 * Cette méthode attends que la queue des tiers soit vide et retourne. Elle permet de s'assurer que tous les tiers enqueués <b>avant</b>
	 * l'appel à la méthode ont bien été indéxés lorsque la méthode retourne.
	 *
	 * @throws InterruptedException si l'attente a été interrompue
	 */
	public void sync() throws InterruptedException {
		while (!queue.isEmpty()) {
			Thread.sleep(10);
		}
	}

	public void setEnabled(boolean e) {
		enabled = e;
	}
}
