package ch.vd.unireg.tache;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.DefaultThreadFactory;
import ch.vd.unireg.common.DefaultThreadNameGenerator;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.type.TypeEtatTache;

public class RecalculTachesProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecalculTachesProcessor.class);
	private static final int BATCH_SIZE = 100;

	/**
	 * Détermine le scope de recalcul des tâches d'envoi/d'annulation de documents
	 */
	public enum Scope {
		PP,
		PM
	}

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TacheService tacheService;
	private final TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	
	private class SyncTask implements Callable<TacheSyncResults> {
		
		private final List<Long> ids;
		private final int percent;
		private final String principal;
		private final StatusManager statusManager;

		private SyncTask(List<Long> ids, int percent, String principal, StatusManager statusManager) {
			this.ids = ids;
			this.percent = percent;
			this.principal = principal;
			this.statusManager = statusManager;
		}

		@Override
		public TacheSyncResults call() {
			AuthenticationHelper.pushPrincipal(principal);
			try {
				final boolean wasEnabled = tacheSynchronizerInterceptor.isEnabled();
				tacheSynchronizerInterceptor.setEnabled(false);
				try {
					statusManager.setMessage("Recalcul en cours...", percent);
					return doRunWithRetry(ids);
				}
				finally {
					tacheSynchronizerInterceptor.setEnabled(wasEnabled);
				}
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	public RecalculTachesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TacheService tacheService,
	                               TacheSynchronizerInterceptor tacheSynchronizerInterceptor) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tacheService = tacheService;
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
	}

	public TacheSyncResults run(boolean existingTasksCleanup, int nbThreads, Scope scope, @Nullable StatusManager s) {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);
		final TacheSyncResults finalResults = new TacheSyncResults(existingTasksCleanup);

		final List<Long> ctbIds = getCtbIds(existingTasksCleanup, scope);
		LOGGER.info(String.format("%d contribuable(s) %s concerné(s) par le traitement de recalcul de tâches", ctbIds.size(), scope));

		final BatchIterator<Long> iterator = new StandardBatchIterator<>(ctbIds, BATCH_SIZE);
		boolean interrupted = false;
		final ExecutorService executorService = Executors.newFixedThreadPool(nbThreads, new DefaultThreadFactory(new DefaultThreadNameGenerator(Thread.currentThread().getName())));
		try {
			final List<Future<TacheSyncResults>> tasks = new LinkedList<>();
			while (iterator.hasNext() && !status.isInterrupted()) {
				final List<Long> ids = iterator.next();
				final int percent = iterator.getPercent();
				tasks.add(executorService.submit(new SyncTask(ids, percent, AuthenticationHelper.getCurrentPrincipal(), status)));
			}

			try {
				final Iterator<Future<TacheSyncResults>> taskIterator = tasks.iterator();
				while (taskIterator.hasNext() && !status.isInterrupted()) {
					final Future<TacheSyncResults> future = taskIterator.next();
					try {
						finalResults.addAll(future.get());
					}
					catch (ExecutionException e) {
						LOGGER.error("Exception lancée par une des sous-tâches de recalcul des tâches", e.getCause());
					}
					catch (InterruptedException e) {
						LOGGER.warn("Thread interrompu", e);
						interrupted = true;
						break;
					}
					finally {
						taskIterator.remove();
					}
				}
			}
			finally {
				// il faut arrêter tous les traitements qui restent en attente
				if (!tasks.isEmpty()) {
					tasks.stream()
							.filter(task -> !task.isDone())
							.forEach(task -> task.cancel(false));
				}
			}
		}
		finally {
			executorService.shutdown();

			// on attend que tout s'arrête
			try {
				//noinspection StatementWithEmptyBody
				while (!executorService.awaitTermination(1, TimeUnit.SECONDS));
			}
			catch (InterruptedException e) {
				LOGGER.warn("Thread interrompu", e);
				interrupted = true;
			}
		}

		finalResults.end();
		finalResults.setInterrupted(status.isInterrupted() || interrupted);
		return finalResults;
	}

	private TacheSyncResults doRunWithRetry(List<Long> ids) {
		try {
			return tacheService.synchronizeTachesDeclarations(ids);
		}
		catch (RuntimeException e) {
			final TacheSyncResults results = new TacheSyncResults(false);
			if (ids.size() == 1) {
				// on ne peut rien faire de plus...
				results.addErrorException(ids.get(0), e);
				LOGGER.error(e.getMessage(), e);
			}
			else {
				// on essaie un par un
				for (Long id : ids) {
					results.addAll(doRunWithRetry(Collections.singletonList(id)));
				}
			}
			return results;
		}
	}

	private List<Long> getCtbIds(final boolean existingTasksCleanup, final Scope scope) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> extractCtbIds(existingTasksCleanup, scope));
	}

	private List<Long> extractCtbIds(boolean existingTasksCleanup, Scope scope) {
		final String ctbClassPart = scope == Scope.PP
				? "(PersonnePhysique, MenageCommun)"
				: "(Entreprise)";


		final String hql;
		if (existingTasksCleanup) {
			hql = String.format("select distinct ctb.id " +
					                    "from Tache as t " +
					                    "join t.contribuable as ctb " +
					                    "where etat = '%s' " +
					                    "and t.annulationDate is null " +
					                    "and type(ctb) in %s " +
					                    "order by ctb.id",
			                    TypeEtatTache.EN_INSTANCE.name(),
			                    ctbClassPart);
		}
		else {
			final String forClassPart = scope == Scope.PP
					? "(ForFiscalPrincipalPP, ForFiscalSecondaire)"
					: "(ForFiscalPrincipalPM, ForFiscalSecondaire)";

			hql = String.format("select distinct ctb.id " +
					                    "from Contribuable as ctb inner " +
					                    "join ctb.forsFiscaux as for " +
					                    "where type(for) in %s " +
					                    "and for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD' " +
					                    "and type(ctb) in %s " +
					                    "order by ctb.id",
		                        forClassPart,
		                        ctbClassPart);
		}

		return hibernateTemplate.executeWithNewSession(session -> {
			final Query query = session.createQuery(hql);
			//noinspection unchecked
			return (List<Long>) query.list();
		});
	}
}
