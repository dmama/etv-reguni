package ch.vd.unireg.indexer.async;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Indexer de tiers utilisé pour l'indexation au fil-de-l'eau des tiers de la base de données.
 */
public class OnTheFlyTiersIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OnTheFlyTiersIndexer.class);

	public static final int BATCH_SIZE = 20;

	private TiersDAO tiersDAO;
	private GlobalTiersIndexerImpl indexer;
	private PlatformTransactionManager transactionManager;
	private SessionFactory sessionFactory;
	private ExecutorService executorService;

	/**
	 * La liste des tâches soumises à exécution.
	 */
	private final List<Task> queuedTasks = new LinkedList<>();

	/**
	 * Demande l'indexation ou la ré-indexation d'un tiers.
	 * <p/>
	 * Cette méthode retourne immédiatement : l'indexation proprement dites est déléguée à un thread asynchrone.
	 *
	 * @param id l'id du tiers à indexer
	 */
	public void schedule(@NotNull Long id) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Ajout de l'id [" + id + "] dans la queue...");
		}
		addTask(Collections.singletonList(id));
	}

	/**
	 * Demande l'indexation ou la ré-indexation de plusieurs tiers.
	 * <p/>
	 * Cette méthode retourne immédiatement : l'indexation proprement dites est déléguée à un thread asynchrone.
	 *
	 * @param ids les ids des tiers à indexer
	 */
	public void schedule(@NotNull Collection<Long> ids) {
		if (ids.size() > BATCH_SIZE) {
			// on découpe par lot de vingt tiers au maximum
			final StandardBatchIterator<Long> batchIterator = new StandardBatchIterator<>(ids, BATCH_SIZE);
			while(batchIterator.hasNext()) {
				schedule(batchIterator.next());
			}
		}
		else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Ajout des tiers = [" + ids.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "] dans la queue...");
			}
			addTask(ids);
		}
	}

	/**
	 * @return le nombre de tiers dont l'indexation a été demandée et qui n'est pas encore terminée.
	 */
	public int getQueueSize() {
		synchronized (queuedTasks) {
			purgeTasks();
			int sum = 0;
			for (Task task : queuedTasks) {
				sum += task.idsCount;
			}
			return sum;
		}
	}

	/**
	 * Attends que tous les tiers dont l'indexation a été demandée aient été indexés. Cette méthode bloque donc tant que la queue d'indexation est pleine.
	 * <p/>
	 * <b>Note:</b> cette méthode n'empêche pas d'autres threads de scheduler l'indexation de nouveau tiers. En d'autres termes, le temps d'attente peu s'allonger indéfiniment.
	 */
	public void sync() {
		LOGGER.trace("Sync de la queue...");
		synchronized (queuedTasks) {
			purgeTasks();
			while (!queuedTasks.isEmpty()) {
				try {
					queuedTasks.wait(10);   // on attend 10 millisecondes, pendant lesquelles de nouvelles tâches peuvent s'ajouter ou se terminer
					purgeTasks();
				}
				catch (InterruptedException e) {
					LOGGER.warn("Attente interrompue.", e);
					return;
				}
			}
		}
		LOGGER.trace("Terminé.");
	}

	/**
	 * Vide la queue d'indexation et annule tous les indexations en cours
	 */
	public void reset() {
		LOGGER.trace("Clear de la queue...");
		synchronized (queuedTasks) {
			purgeTasks();
			queuedTasks.forEach(task -> task.future.cancel(true));
			queuedTasks.clear();
		}
		LOGGER.trace("Terminé.");
	}

	/**
	 * Relâche les ressources et arrête complétement l'indexer
	 */
	public void destroy() {
		// Vide la queue et arrête les threads
		reset();
	}

	private static class Task {
		private final int idsCount;
		@NotNull
		private final Future<?> future;

		public Task(int idsCount, @NotNull Future<?> future) {
			this.idsCount = idsCount;
			this.future = future;
		}
	}

	/**
	 * Ajoute une nouvelle tâche d'indexation
	 *
	 * @param ids les ids des tiers à indexer
	 */
	private void addTask(@NotNull Collection<Long> ids) {
		if (ids.isEmpty()) {
			// rien à faire
			return;
		}
		final List<Long> idsCopy = new ArrayList<>(ids); // note : on fait une copie de la liste pour être sûr qu'elle ne soit pas vidée dans notre dos

		// on soumet le traitement à l'exécuteur
		final Future<?> future = executorService.submit(() -> indexAll(idsCopy));

		// on mémorise la nouvelle tâche
		synchronized (queuedTasks) {
			queuedTasks.add(new Task(ids.size(), future));
			// on profite de purger la queue des tâches terminées ou annulées
			purgeTasks();
		}
	}

	/**
	 * Supprime les tâches terminées ou annulées
	 */
	private void purgeTasks() {
		queuedTasks.removeIf(t -> t.future.isDone() || t.future.isCancelled());
	}

	/**
	 * Indexe les tiers dont les ids sont spécifiés.
	 * <p/>
	 * <b>Note :</b> cette méthode s'exécute dans un thread séparés
	 *
	 * @param tiersIds la collection des ids de tiers à indexer
	 */
	private void indexAll(@NotNull final Collection<Long> tiersIds) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexation des tiers = " + Arrays.toString(tiersIds.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			// On crée à la main une nouvelle session hibernate en ayant pris soin de désactiver l'intercepteur. Cela permet de désactiver
			// la validation des tiers, et de flagger comme 'dirty' même les tiers qui ne valident pas. Autrement, le premier tiers qui ne valide pas
			// fait péter une exception, qui remonte jusqu'à la méthode 'run' du thread et qui provoque l'arrêt immédiat du thread !
			final Switchable interceptorSwitch = (Switchable) sessionFactory.getSessionFactoryOptions().getInterceptor();
			final boolean enabled = interceptorSwitch.isEnabled();

			interceptorSwitch.setEnabled(false);
			try (Session session = sessionFactory.openSession()) {
				session.setFlushMode(FlushModeType.COMMIT);
				final List<Tiers> list;

				if (tiersIds.size() == 1) {
					final Tiers tiers = session.get(Tiers.class, tiersIds.iterator().next());
					if (tiers != null) {
						list = new ArrayList<>(1);
						list.add(tiers);
					}
					else {
						list = null;
					}
				}
				else {
					final Query query = session.createQuery("from Tiers t where t.id in (:ids)");
					query.setParameterList("ids", tiersIds);
					//noinspection unchecked
					list = query.list();
				}

				if (list != null) {
					indexInSession(list, session);
				}
				session.flush();
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			finally {
				interceptorSwitch.setEnabled(enabled);
			}

			return null;
		});
	}

	/**
	 * Indexe les tiers spécifié dans une session Hibernate particulière
	 *
	 * @param tiers   la liste des tiers à indexer
	 * @param session la session Hibernate à utiliser
	 */
	private void indexInSession(@NotNull List<Tiers> tiers, Session session) {

		if (tiers.isEmpty()) {
			return;
		}

		try {
			// on index le tiers
			indexer.indexTiers(tiers, true, true);
			tiersDAO.setDirtyFlag(extractIds(tiers), false, session);
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			// la plupart des tiers ont pu être indexés...
			final Set<Long> indexedIds = new HashSet<>(extractIds(tiers));

			// ...sauf ceux-ci
			final Set<Long> inErrorIds = new HashSet<>();
			final List<Pair<Long, Exception>> list = e.getExceptions();
			for (Pair<Long, Exception> p : list) {
				final Long tiersId = p.getLeft();
				if (tiersId != null) {
					inErrorIds.add(tiersId);
				}
			}
			indexedIds.removeAll(inErrorIds);

			// reset le flag dirty de tous les tiers qui ont été indexés
			tiersDAO.setDirtyFlag(indexedIds, false, session);
			// flag tous les tiers qui n'ont pas pu être indexés comme dirty, notamment ceux qui ne l'étaient pas avant
			tiersDAO.setDirtyFlag(inErrorIds, true, session);
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers = " + buildTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			tiersDAO.setDirtyFlag(extractIds(tiers), true, session);
		}
	}

	@NotNull
	private static List<Long> extractIds(@NotNull List<Tiers> tiers) {
		return tiers.stream()
				.filter(Objects::nonNull)
				.map(Tiers::getNumero)
				.collect(Collectors.toList());
	}

	@NotNull
	private static String buildTiersNumeros(@NotNull List<Tiers> list) {
		return "[" + list.stream()
				.map(Tiers::getNumero)
				.map(String::valueOf)
				.collect(Collectors.joining(", ")) + "]";
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setIndexer(GlobalTiersIndexerImpl indexer) {
		this.indexer = indexer;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
