package ch.vd.uniregctb.indexer.async;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Met-à-jour l'indexe des tiers de manière asynchrone.
 */
public class AsyncTiersIndexer {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = Logger.getLogger(AsyncTiersIndexer.class);

	private final GlobalTiersIndexer indexer;
	private final PlatformTransactionManager transactionManager;
	private final SessionFactory sessionFactory;

	protected boolean isInit = false;
	private boolean enabled = true;
	private final int nbThreads;
	private final int queueByThreadSize;
	private final Mode mode;

	private ArrayList<AsyncTiersIndexerThread> threads;
	protected BlockingQueue<Long> queue;

	private long totalCpuTime;
	private long totalUserTime;
	private long totalExecTime;

	public AsyncTiersIndexer(GlobalTiersIndexer indexer, PlatformTransactionManager transactionManager, SessionFactory sessionFactory, int nbThreads, int queueByThreadSize, Mode mode) {
		this.indexer = indexer;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;

		this.queueByThreadSize = queueByThreadSize;
		this.nbThreads = nbThreads;
		this.mode = mode;
	}

	public void initialize() {

		// Le flag doit etre setté avant le démarrage des threads sinon elles se terminent tout de suite...
		isInit = true;

		threads = new ArrayList<AsyncTiersIndexerThread>();
		queue = new ArrayBlockingQueue<Long>(queueByThreadSize * nbThreads);

		for (int i = 0; i < nbThreads; i++) {
			AsyncTiersIndexerThread t = new AsyncTiersIndexerThread(this);
			threads.add(t);
			t.setName("Async-" + i);
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
		queue = null;
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
	 * @throws InterruptedException
	 */
	public void flushAndWait() throws InterruptedException {
		while (queue.size() > 0) {
			Thread.sleep(500);
		}
	}

	protected void delegateRun() {
		try {
			AuthenticationHelper.setPrincipal("[ASYNC Indexer]");
			indexer.setOnTheFlyIndexation(false); // on désactive l'indexation pour ce thread

			// Indexe tous les tiers dans la queue en procédant par batchs, ceci pour limiter le nombre d'objets en mémoire
			while (isInit || (queue != null && !queue.isEmpty())) {
				TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						indexBatch();
						return null;
					}
				});
			}
		}
		catch (Exception e) {
			LOGGER.error("Exception catchée au niveau du thread d'indexation. Ce thread est interrompu !", e);
			throw new RuntimeException(e);
		}
		finally {
			indexer.setOnTheFlyIndexation(true);
			AuthenticationHelper.resetAuthentication();
		}
	}

	/**
	 * @return le prochain id à indexer, ou <b>null</b> s'il n'y a plus rien à faire
	 */
	private Long nextId() {
		if (queue == null) {
			return null;
		}
		try {
			return queue.poll(1, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			LOGGER.error(e, e);
			return null;
		}
	}

	/**
	 * Indexe les BATCH_SIZE prochains tiers et rend la main à l'appelant (même s'il reste des tiers dans la queue).
	 */
	private void indexBatch() {

		final List<Long> indexed = new ArrayList<Long>();
		final List<Long> dirties = new ArrayList<Long>();

		/*
		 * On crée à la main une nouvelle session hibernate avec un intercepteur vide (HibernateFakeInterceptor). Cela permet de désactiver
		 * la validation des tiers, et de flagger comme 'dirty' même les tiers qui ne valident pas. Autrement, le premier tiers qui ne valide pas
		 * fait péter une exception, qui remonte jusqu'à la méthode 'run' du thread et qui provoque l'arrêt immédiat du thread !
		 */
		Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
		try {
			session.setFlushMode(FlushMode.MANUAL);

			final List<Tiers> list = getNextBatch(session);

			indexTiers(list);

			for (Tiers t : list) {
				if (t.isDirty()) {
					dirties.add(t.getNumero());
				}
				else {
					indexed.add(t.getNumero());
				}
			}

			// on ne modifie pas la base et on met-à-jour le flag 'dirty' dans une seconde session,
			// parce que Hibernate prend beaucoup de temps pour parser toutes les instances chargées
			session.clear();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}
		finally {
			session.close();
		}

		// Met-à-jour les flags 'dirty'
		session = sessionFactory.openSession(new HibernateFakeInterceptor());
		try {
			if (!dirties.isEmpty()) {
				SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = 1 where NUMERO in (:ids)");
				query.setParameterList("ids", dirties);
				query.executeUpdate();
			}

			if (!indexed.isEmpty()) {
				SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = 0 where NUMERO in (:ids)");
				query.setParameterList("ids", indexed);
				query.executeUpdate();
			}
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}
		finally {
			session.close();
		}
	}

	private List<Tiers> getNextBatch(Session session) {

		final List<Tiers> list = new ArrayList<Tiers>(BATCH_SIZE);
		
		for (int i = 0; i < BATCH_SIZE; ++i) {
			// On fait du polling pour permettre au thread de s'arrêter si nécessaire
			Long id = nextId();
			if (id == null) {
				break;
			}

			final Tiers tiers = (Tiers) session.get(Tiers.class, id);
			list.add(tiers);
		}
		return list;
	}

	private void indexTiers(List<Tiers> tiers) {

		if (tiers == null || tiers.isEmpty()) {
			return;
		}

		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("ASYNC indexation des tiers n° " + getTiersNumeros(tiers) + " (La queue contient encore " + queue.size()
						+ " tiers a indexer)");
			}
			// on n'indexe pas les tiers liés au tiers courant parce qu'on utilise l'async tiers indexer que lorsqu'on veut indexer toute la
			// base de données : les tiers liés vont de toutes façons se faire indexer pour eux-même.
			final boolean followDependents = false;

			// on n'enlève pas préalablement les données indexées en mode FULL et INCREMENTAL, parce que - par définition - ces données
			// n'existent pas dans ces modes-là.
			final boolean removeBefore = (mode == Mode.DIRTY_ONLY);

			indexer.indexTiers(tiers, removeBefore, followDependents);

			for (Tiers t : tiers) {
				if (t.isDirty()) {
					t.setIndexDirty(false);
				}
			}
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}
			final List<Pair<Tiers, Exception>> list = e.getExceptions();
			for (Pair<Tiers, Exception> p : list) {
				Tiers t = p.getFirst();
				if (t != null && !t.isDirty()) {
					t.setIndexDirty(true);
				}
			}
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers n°" + getTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}
			for (Tiers t : tiers) {
				if (!t.isDirty()) {
					t.setIndexDirty(true);
				}
			}
		}
	}

	private static String getTiersNumeros(List<Tiers> list) {
		StringBuilder builder = new StringBuilder("{");
		for (Tiers t : list) {
			builder.append(t.getNumero()).append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean e) {
		enabled = e;
	}

}
