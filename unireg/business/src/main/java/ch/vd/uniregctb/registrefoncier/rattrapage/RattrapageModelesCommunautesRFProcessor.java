package ch.vd.uniregctb.registrefoncier.rattrapage;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.processor.CommunauteRFProcessor;

/**
 * Ce processeur réapplique les règles de regroupement avec les modèles de communauté sur toutes les communautés existantes.
 */
public class RattrapageModelesCommunautesRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RattrapageModelesCommunautesRFProcessor.class);

	private final AyantDroitRFDAO ayantDroitRFDAO;
	private final CommunauteRFProcessor communauteProcessor;
	private final PlatformTransactionManager transactionManager;

	public RattrapageModelesCommunautesRFProcessor(AyantDroitRFDAO ayantDroitRFDAO, CommunauteRFProcessor communauteProcessor, PlatformTransactionManager transactionManager) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.communauteProcessor = communauteProcessor;
		this.transactionManager = transactionManager;
	}

	public RattrapageModelesCommunautesRFProcessorResults process(int nbThreads, @Nullable StatusManager sm) {

		final StatusManager statusManager = (sm == null ? new LoggingStatusManager(LOGGER) : sm);

		// on va chercher les ids de toutes les communautés
		final List<Long> ids = findIdsCommunautes();
//		final List<Long> ids = Collections.singletonList(189974466L);

		final RattrapageModelesCommunautesRFProcessorResults rapportFinal = new RattrapageModelesCommunautesRFProcessorResults(nbThreads);

		// on traite chaque communauté
		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, RattrapageModelesCommunautesRFProcessorResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, RattrapageModelesCommunautesRFProcessorResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> communauteIds, RattrapageModelesCommunautesRFProcessorResults rapport) throws Exception {
				first.set(communauteIds.get(0));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing community ids={}", Arrays.toString(communauteIds.toArray()));
				}
				statusManager.setMessage("Traitement des communautés...", monitor.getProgressInPercent());
				communauteIds.forEach(id -> processCommunaute(id, rapport));
				return !statusManager.isInterrupted();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long id = first.get();
					LOGGER.warn("Erreur pendant le traitement de la communauté n°" + id, e);
				}
			}

			@Override
			public RattrapageModelesCommunautesRFProcessorResults createSubRapport() {
				return new RattrapageModelesCommunautesRFProcessorResults(nbThreads);
			}
		}, monitor);

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite la communauté : met-à-jour les regroupements vers les modèles de communauté si nécessaire.
	 */
	private void processCommunaute(long id, RattrapageModelesCommunautesRFProcessorResults rapport) {

		rapport.addProcessed();

		final CommunauteRF communaute = (CommunauteRF) ayantDroitRFDAO.get(id);
		if (communaute == null) {
			throw new IllegalArgumentException("La communauté avec l'id=[" + id + "] n'existe pas");
		}

		if (communauteProcessor.process(communaute)) {
			rapport.addUpdated(communaute);
		}
	}

	/**
	 * @return les ids de toutes les communautés.
	 */
	private List<Long> findIdsCommunautes() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> ayantDroitRFDAO.findCommunautesIds());
	}
}
