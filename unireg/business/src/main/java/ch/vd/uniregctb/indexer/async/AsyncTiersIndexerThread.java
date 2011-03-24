package ch.vd.uniregctb.indexer.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.uniregctb.tiers.Tiers;


public class AsyncTiersIndexerThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(AsyncTiersIndexerThread.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final GlobalTiersIndexerImpl indexer;
	private final SessionFactory sessionFactory;
	private final Dialect dialect;

	private final GlobalTiersIndexer.Mode mode;
	private boolean shutdown = false;
	private final BlockingQueue<Long> queue;

	private final Object queueDone = new Object();

	private long executionTime = 0;

	/**
	 * Construit un thread d'indexation qui consomme les ids des tiers à indexer à partir d'une queue.
	 *
	 * @param queue              la queue dans laquelle les ids des tiers à indexer doivent être insérés.
	 * @param mode               le mode d'indexation voulu. Renseigné dans le cas d'une réindexation complète ou partielle; ou <b>null</b> dans le cas d'une indexation au fil de l'eau des tiers.
	 * @param globalTiersIndexer l'indexer des tiers
	 * @param sessionFactory     la session factory hibernate
	 * @param transactionManager le transaction manager
	 * @param dialect            le dialect hibernate utilisé
	 */
	public AsyncTiersIndexerThread(BlockingQueue<Long> queue, GlobalTiersIndexer.Mode mode, GlobalTiersIndexerImpl globalTiersIndexer, SessionFactory sessionFactory,
	                               PlatformTransactionManager transactionManager, Dialect dialect) {
		this.indexer = globalTiersIndexer;
		this.queue = queue;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;
		this.mode = mode;
		this.dialect = dialect;
		Assert.notNull(this.indexer);
		Assert.notNull(this.queue);
		Assert.notNull(this.transactionManager);
		Assert.notNull(this.sessionFactory);
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		try {
			delegateRun();
		}
		catch (Exception e) {
			LOGGER.error(e);
		}
		finally {
			executionTime = System.nanoTime() - start;
			LOGGER.info("Arrêt du thread.");
		}
	}

	/**
	 * Demande au thread de s'arrêter. Le thread termine de vider la queue dans tous les cas.
	 */
	public void shutdown() {
		shutdown = true;
	}

	/**
	 * Attends que tous les tiers dont l'indexation a été demandée aient été indexés.
	 */
	public void sync() {
		try {
			synchronized (queueDone) {
				queueDone.wait();
			}
		}
		catch (InterruptedException e) {
			// on ignore cette exception
		}
	}

	/**
	 * Interrompt l'indexation courante et retourne lorsque le thread est de nouveau en attente
	 */
	public void reset() {
		// msi (12.05.2010) appeler 'interrupt' n'est pas une bonne idée: ça fout le bordel dans la gestion des locks de Lucene
		// interrupt(); // on interrompt le poll sur la queue
		sync(); // on attend que le thread soit de nouveau en attente
	}

	private void delegateRun() {
		try {
			AuthenticationHelper.setPrincipal("[ASYNC Indexer]");

			// Indexe tous les tiers dans la queue en procédant par batchs, ceci pour limiter le nombre d'objets en mémoire
			while (!shutdown || !queue.isEmpty()) {
				final List<Long> batch = nextBatch();
				if (batch != null) {
					indexBatch(batch);
				}
				else {
					// le batch est null, on continue à boucler. Il n'y a pas besoin d'attendre car la méthode nextBatch le fait déjà.
					notifyQueueDone();
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Exception catchée au niveau du thread d'indexation. Ce thread est interrompu !", e);
			throw new RuntimeException(e);
		}
		finally {
			notifyQueueDone();
			AuthenticationHelper.resetAuthentication();
		}
	}

	private void notifyQueueDone() {
		synchronized (queueDone) {
			queueDone.notifyAll(); // notifie les threads en attente sur 'sync' qu'on a fini (momentanément) d'indexer tous les tiers de la queue
		}
	}

	private void indexBatch(final List<Long> batch) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback() {
			@SuppressWarnings({"unchecked"})
			public Object doInTransaction(TransactionStatus status) {
				/*
				 * On crée à la main une nouvelle session hibernate avec un intercepteur vide (HibernateFakeInterceptor). Cela permet de désactiver
				 * la validation des tiers, et de flagger comme 'dirty' même les tiers qui ne valident pas. Autrement, le premier tiers qui ne valide pas
				 * fait péter une exception, qui remonte jusqu'à la méthode 'run' du thread et qui provoque l'arrêt immédiat du thread !
				 */
				Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
				try {
					session.setFlushMode(FlushMode.MANUAL);
					final List<Tiers> list;

					if (batch.size() == 1) {
						final Tiers tiers = (Tiers) session.get(Tiers.class, batch.get(0));
						if (tiers != null) {
							list = new ArrayList<Tiers>(1);
							list.add(tiers);
						}
						else {
							list = null;
						}
					}
					else {
						final Query query = session.createQuery("from Tiers t where t.id in (:ids)");
						query.setParameterList("ids", batch);
						list = query.list();
					}

					indexTiers(list, session);
					session.flush();
				}
				catch (Exception e) {
					LOGGER.error(e, e);
				}
				finally {
					session.close();
				}
				return null;
			}
		});
	}

	/**
	 * @return le prochain lot d'ids à indexer, ou <b>null</b> s'il n'y a (momentanément) plus rien à faire (timeout de 0.1 seconde)
	 */
	private List<Long> nextBatch() {

		List<Long> batch = null;

		for (int i = 0; i < BATCH_SIZE; i++) {
			final Long id = nextId();
			if (id == null) {
				// la queue est vide (peut-être momentanément), on va comme ça avec ce batch
				break;
			}
			if (batch == null) {
				batch = new ArrayList<Long>(BATCH_SIZE);
			}
			batch.add(id);
		}

		return batch;
	}

	/**
	 * @return le prochain id à indexer, ou <b>null</b> s'il n'y a (momentanément) plus rien à faire (timeout de 0.1 seconde)
	 */
	private Long nextId() {
		try {
			return queue.poll(100, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			interrupted(); // reset le flag
			//LOGGER.error(e, e);
			return null;
		}
	}

	private void indexTiers(List<Tiers> tiers, Session session) {

		if (tiers == null || tiers.isEmpty()) {
			return;
		}

		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("ASYNC indexation des tiers n° " + buildTiersNumeros(tiers) + " (La queue contient encore " + queue.size()
						+ " tiers a indexer)");
			}
			// on n'indexe pas les tiers liés au tiers courant lorsqu'on veut indexer toute ou une fraction déterminée
			// de la base de données : les tiers liés vont de toutes façons se faire indexer pour eux-même.
			final boolean followDependents = (mode == null);

			// on n'enlève pas préalablement les données indexées en mode FULL et INCREMENTAL, parce que - par définition -
			// ces données n'existent pas dans ces modes-là.
			final boolean removeBefore = (mode != GlobalTiersIndexer.Mode.FULL && mode != GlobalTiersIndexer.Mode.INCREMENTAL);

			indexer.indexTiers(tiers, removeBefore, followDependents);

			setDirtyFlag(extractIds(tiers), false, session);
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}

			// la plupart des tiers ont pu être indexés...
			final Set<Long> indexedIds = new HashSet<Long>(extractIds(tiers));

			// ...sauf ceux-ci
			final Set<Long> inErrorIds = new HashSet<Long>();
			final List<Pair<Long, Exception>> list = e.getExceptions();
			for (Pair<Long, Exception> p : list) {
				final Long tiersId = p.getFirst();
				if (tiersId != null) {
					inErrorIds.add(tiersId);
				}
			}
			indexedIds.removeAll(inErrorIds);

			setDirtyFlag(indexedIds, false, session); // reset le flag dirty de tous les tiers qui ont été indexés
			setDirtyFlag(inErrorIds, true, session); // flag tous les tiers qui n'ont pas pu être indexés comme dirty, notamment ceux qui ne l'étaient pas avant
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers n°" + buildTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}

			setDirtyFlag(extractIds(tiers), true, session);
		}
	}

	private void setDirtyFlag(Collection<Long> ids, boolean flag, Session session) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		final SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(flag) + " where NUMERO in (:ids)");
		query.setParameterList("ids", ids);
		query.executeUpdate();

		if (!flag) {
			// [UNIREG-1979] On remet aussi à zéro tous les tiers dont la date 'reindex_on' est atteinte aujourd'hui
			final SQLQuery q = session.createSQLQuery("update TIERS set REINDEX_ON = null where REINDEX_ON is not null and REINDEX_ON <= :today and NUMERO in (:ids)");
			q.setParameter("today", RegDate.get().index());
			q.setParameterList("ids", ids);
			q.executeUpdate();
		}
	}

	private List<Long> extractIds(List<Tiers> tiers) {
		if (tiers == null || tiers.isEmpty()) {
			return null;
		}
		final List<Long> ids = new ArrayList<Long>(tiers.size());
		for (Tiers t : tiers) {
			if (t != null) {
				ids.add(t.getNumero());
			}
		}
		return ids;
	}

	private static String buildTiersNumeros(List<Tiers> list) {
		StringBuilder builder = new StringBuilder("{");
		for (int i = 0, listSize = list.size(); i < listSize; i++) {
			final Tiers t = list.get(i);
			builder.append(t.getNumero());
			if (i < listSize - 1) {
				builder.append(", ");
			}
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * @return le temps d'exécution en nano-secondes
	 */
	public long getExecutionTime() {
		return executionTime;
	}
}
