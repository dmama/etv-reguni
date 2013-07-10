package ch.vd.uniregctb.tache;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeEtatTache;

public class RecalculTachesProcessor {

	private static final Logger LOGGER = Logger.getLogger(RecalculTachesProcessor.class);
	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TacheService tacheService;
	private final TacheSynchronizerInterceptor tacheSynchronizerInterceptor;

	public RecalculTachesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TacheService tacheService,
	                               TacheSynchronizerInterceptor tacheSynchronizerInterceptor) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tacheService = tacheService;
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
	}

	public TacheSyncResults run(boolean existingTasksCleanup, @Nullable StatusManager s) {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);
		final List<Long> ctbIds = getCtbIds(existingTasksCleanup);
		LOGGER.info(String.format("%d contribuable(s) concernés par le traitement de recalcul de tâche", ctbIds.size()));

		final BatchIterator<Long> iterator = new StandardBatchIterator<>(ctbIds, BATCH_SIZE);
		final TacheSyncResults results = new TacheSyncResults(existingTasksCleanup);

		final boolean wasEnabled = tacheSynchronizerInterceptor.isEnabled();
		tacheSynchronizerInterceptor.setEnabled(false);
		try {
			while (iterator.hasNext()) {
				final List<Long> ids = iterator.next();
				status.setMessage("Recalcul en cours...", iterator.getPercent());
				doRunWithRetry(results, ids);
				if (status.interrupted()) {
					break;
				}
			}
		}
		finally {
			tacheSynchronizerInterceptor.setEnabled(wasEnabled);
		}
		results.end();
		results.setInterrupted(status.interrupted());
		return results;
	}

	private void doRunWithRetry(TacheSyncResults results, List<Long> ids) {
		try {
			results.addAll(tacheService.synchronizeTachesDIs(ids));
		}
		catch (RuntimeException e) {
			if (ids.size() == 1) {
				// on ne peut rien faire de plus...
				results.addErrorException(ids.get(0), e);
				LOGGER.error(e, e);
			}
			else {
				// on essaie un par un
				for (Long id : ids) {
					doRunWithRetry(results, Arrays.asList(id));
				}
			}
		}
	}

	private List<Long> getCtbIds(final boolean existingTasksCleanup) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return extractCtbIds(existingTasksCleanup);
			}
		});
	}

	private List<Long> extractCtbIds(boolean existingTasksCleanup) {
		final String hql;
		if (existingTasksCleanup) {
			hql = "select distinct t.contribuable.id from Tache as t where etat = '" + TypeEtatTache.EN_INSTANCE.name() + "' order by t.contribuable.id";
		}
		else {
			hql = "select distinct ctb.id from Contribuable as ctb inner join ctb.forsFiscaux as for where for.class in (ForFiscalPrincipal, ForFiscalSecondaire) and for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD' order by ctb.id";
		}

		return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				return query.list();
			}
		});
	}
}
