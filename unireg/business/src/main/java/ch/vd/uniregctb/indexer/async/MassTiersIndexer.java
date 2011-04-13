package ch.vd.uniregctb.indexer.async;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
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

	private ArrayList<AsyncTiersIndexerThread> threads;
	private final BlockingQueue<Long> queue;

	private long totalCpuTime;
	private long totalUserTime;
	private long totalExecTime;

	public MassTiersIndexer(GlobalTiersIndexerImpl indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, int nbThreads, int queueByThreadSize, Mode mode,
	                        Dialect dialect) {

		// Le flag doit etre setté avant le démarrage des threads sinon elles se terminent tout de suite...
		isInit = true;

		threads = new ArrayList<AsyncTiersIndexerThread>();
		queue = new ArrayBlockingQueue<Long>(queueByThreadSize * nbThreads);

		for (int i = 0; i < nbThreads; i++) {
			AsyncTiersIndexerThread t = new AsyncTiersIndexerThread(queue, mode, indexer, sessionFactory, transactionManager, dialect);
			threads.add(t);
			t.setName("Mass-" + i);
			LOGGER.info("Démarrage du thread " + t.getName());
			t.start();
		}
	}

	public void clearQueue() {
		Assert.isTrue(isInit);
		queue.clear();
	}

	public void terminate() throws Exception {

		if (!isInit) {
			return;
		}

		isInit = false;

		for (AsyncTiersIndexerThread thread : threads) {
			thread.shutdown();
		}

		boolean interrupted = false;

		ThreadMXBean mXBean = ManagementFactory.getThreadMXBean();
		for (AsyncTiersIndexerThread thread : threads) {
			long cpuTime = mXBean.getThreadCpuTime(thread.getId());
			long userTime = mXBean.getThreadUserTime(thread.getId());

			thread.join(10000);
			if (thread.isAlive()) {
				LOGGER.warn("Interruption forcée du thread " + thread.getName() + " qui ne s'est pas arrêté après 10 secondes d'attente.");
				thread.interrupt();
				interrupted = true;
			}

			long execTime = thread.getExecutionTime();
			totalCpuTime += cpuTime;
			totalUserTime += userTime;
			totalExecTime += execTime;
		}

		if (!interrupted) { // Si un thread a été interrompu, il va rester des tiers dans la queue, c'est certain. Inutile de faire sauter
							// un assert pour rien.
			Assert.isEqual(0, queue.size());
		}

		threads = null;
		queue.clear();
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
