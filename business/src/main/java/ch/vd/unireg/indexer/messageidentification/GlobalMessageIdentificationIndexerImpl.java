package ch.vd.unireg.indexer.messageidentification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexableData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.utils.LogLevel;

public class GlobalMessageIdentificationIndexerImpl implements GlobalMessageIdentificationIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMessageIdentificationIndexerImpl.class);

	private static final int BATCH_SIZE = 100;

	private GlobalIndexInterface globalIndex;
	private PlatformTransactionManager transactionManager;
	private IdentCtbDAO identCtbDAO;
	private SessionFactory sessionFactory;

	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void overwriteIndex() {
		globalIndex.overwriteIndex();
	}

	@Override
	public void reindex(long id) {
		reindex(id, true);
	}

	private static final class IndexationResults implements BatchResults<Long, IndexationResults> {

		private int nbIndexed = 0;

		@Override
		public void addErrorException(Long element, Exception e) {
			LOGGER.error(String.format("Indexation impossible de la demande d'identification de contribuable %d", element), e);
		}

		@Override
		public void addAll(IndexationResults right) {
			nbIndexed += right.nbIndexed;
		}
	}

	@Override
	public int indexAllDatabase(@Nullable StatusManager s, int nbThreads) throws IndexerException {

		final StatusManager statusManager = s == null ? new LoggingStatusManager(LOGGER, LogLevel.Level.DEBUG) : s;

		// on recommence à zéro
		statusManager.setMessage("Effacement du repertoire d'indexation");
		overwriteIndex();

		// allons chercher les ids des éléments à indexer
		statusManager.setMessage("Récupération des messages à indexer...");
		final List<Long> ids = getAllIds();
		final String msgStatus = String.format("Indexation des %d messages d'identification", ids.size());

		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final IndexationResults results = new IndexationResults();
		final ParallelBatchTransactionTemplateWithResults<Long, IndexationResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads,
		                                                                                                                                        Behavior.REPRISE_AUTOMATIQUE, transactionManager,
		                                                                                                                                        statusManager, AuthenticationInterface.INSTANCE);
		template.execute(results, new BatchWithResultsCallback<Long, IndexationResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, IndexationResults rapport) throws Exception {
				statusManager.setMessage(msgStatus, monitor.getProgressInPercent());
				final List<IdentificationContribuable> list = new ArrayList<>(batch.size());
				for (Long idElement : batch) {
					list.add(identCtbDAO.get(idElement));
				}
				rapport.nbIndexed += list.size();
				reindex(list, false);
				return true;
			}

			@Override
			public IndexationResults createSubRapport() {
				return new IndexationResults();
			}
		}, monitor);

		statusManager.setMessage("Suppression des doublons...");
		globalIndex.deleteDuplicate();

		return results.nbIndexed;
	}

	private void reindex(long id, boolean eraseBeforeIndexing) {
		final IdentificationContribuable msg = identCtbDAO.get(id);
		reindex(Collections.singletonList(msg), eraseBeforeIndexing);
	}

	private void reindex(List<IdentificationContribuable> list, boolean eraseBeforeIndexing) {
		final List<IndexableData> data = new ArrayList<>(list.size());
		for (IdentificationContribuable msg : list) {
			data.add(new MessageIdentificationIndexable(msg).getIndexableData());
		}
		try {
			if (eraseBeforeIndexing) {
				globalIndex.removeThenIndexEntities(data);
			}
			else {
				globalIndex.indexEntities(data);
			}
		}
		catch (Exception e){
			LOGGER.error("Exception lors de l'indexation de message d'identification: "+e.getMessage(),e);
		}
	}

	private List<Long> getAllIds() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(status -> {
			final Session session = sessionFactory.getCurrentSession();
			final Query query = session.createQuery("SELECT i.id FROM IdentificationContribuable i ORDER BY id ASC");
			//noinspection unchecked

			return query.list();
		});
	}
}
