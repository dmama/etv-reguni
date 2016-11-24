package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.SubStatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.processor.AyantDroitRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.DroitRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.SurfaceAuSolRFProcessor;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Processeur responsable de traiter les mutations du registre foncier.
 */
public class DataRFMutationsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataRFMutationsProcessor.class);

	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final MutationRFProcessor communeRFProcessor;
	private final MutationRFProcessor immeubleRFProcessor;
	private final AyantDroitRFProcessor ayantDroitRFProcessor;
	private final DroitRFProcessor droitRFProcessor;
	private final SurfaceAuSolRFProcessor surfaceAuSolRFProcessor;

	public DataRFMutationsProcessor(@NotNull EvenementRFMutationDAO evenementRFMutationDAO,
	                                @NotNull MutationRFProcessor communeRFProcessor,
	                                @NotNull MutationRFProcessor immeubleRFProcessor,
	                                @NotNull AyantDroitRFProcessor ayantDroitRFProcessor,
	                                @NotNull DroitRFProcessor droitRFProcessor,
	                                @NotNull SurfaceAuSolRFProcessor surfaceAuSolRFProcessor,
	                                @NotNull PlatformTransactionManager transactionManager) {
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.ayantDroitRFProcessor = ayantDroitRFProcessor;
		this.immeubleRFProcessor = immeubleRFProcessor;
		this.droitRFProcessor = droitRFProcessor;
		this.surfaceAuSolRFProcessor = surfaceAuSolRFProcessor;
		this.transactionManager = transactionManager;
		this.communeRFProcessor = communeRFProcessor;
	}

	/**
	 * Traite tous les mutations à l'état A_TRAITER de l'import spécifié
	 *
	 * @param importId      l'id d'un import du registre foncier
	 * @param nbThreads     le nombre de threads à utiliser pour le traitement
	 * @param statusManager un status manager pour suivre la progression du traitement
	 */
	public void processImport(long importId, int nbThreads, @Nullable StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}

		checkPreconditions(importId);

		processMutations(importId, EvenementRFMutation.TypeEntite.COMMUNE, nbThreads, new SubStatusManager(0, 25, statusManager));
		processMutations(importId, EvenementRFMutation.TypeEntite.IMMEUBLE, nbThreads, new SubStatusManager(0, 25, statusManager));
		processMutations(importId, EvenementRFMutation.TypeEntite.AYANT_DROIT, nbThreads, new SubStatusManager(25, 50, statusManager));
		processMutations(importId, EvenementRFMutation.TypeEntite.DROIT, nbThreads, new SubStatusManager(50, 75, statusManager));
		processMutations(importId, EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, nbThreads, new SubStatusManager(75, 100, statusManager));
	}

	private void checkPreconditions(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Long nextMutationsToProcess = evenementRFMutationDAO.findNextMutationsToProcess();
				if (nextMutationsToProcess != null && nextMutationsToProcess != importId) {
					throw new IllegalArgumentException("Les mutations de l'import RF avec l'id = [" + importId + "] ne peuvent être traitées car les mutations de l'import RF avec l'id = [" + nextMutationsToProcess + "] n'ont pas été traitées.");
				}
			}
		});
	}

	private void processMutations(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, int nbThreads, @NotNull final StatusManager statusManager) {

		final List<Long> ids = findIdsMutationsATraiter(importId, typeEntite);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Mutations to process = {}", Arrays.toString(ids.toArray()));
		}

		final MutationRFProcessor proc = getProcessor(typeEntite);

		// TODO (msi) générer un rapport
		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplate<Long> template = new ParallelBatchTransactionTemplate<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(new BatchCallback<Long>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> mutationsIds) throws Exception {
				first.set(mutationsIds.get(0));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing mutations ids={}", Arrays.toString(mutationsIds.toArray()));
				}
				statusManager.setMessage("Traitement des mutations " + typeEntite.name() + "...", monitor.getProgressInPercent());
				mutationsIds.stream()
						.map(id -> getMutation(id))
						.forEach(mut -> processMutation(mut, proc));
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = first.get();
					LOGGER.warn("Erreur pendant le traitement de la mutation n°" + mutId, e);
					if (mutId != null) {
						updateMutation(mutId, e);
					}
				}
			}
		}, monitor);
	}

	private void processMutation(@NotNull EvenementRFMutation mut, @NotNull MutationRFProcessor proc) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing mutation id=[{}]", mut.getId());
		}

		proc.process(mut);

		// on met-à-jour le statut de la mutation
		mut.setEtat(EtatEvenementRF.TRAITE);
	}

	@NotNull
	private EvenementRFMutation getMutation(Long id) {
		final EvenementRFMutation mutation = evenementRFMutationDAO.get(id);
		if (mutation == null) {
			throw new IllegalArgumentException("La mutation RF avec l'id=[" + id + "] n'existe pas.");
		}
		return mutation;
	}

	@NotNull
	private MutationRFProcessor getProcessor(@NotNull EvenementRFMutation.TypeEntite typeEntite) {
		switch (typeEntite) {
		case AYANT_DROIT:
			return ayantDroitRFProcessor;
		case BATIMENT:
			// TODO (msi)
			throw new NotImplementedException();
		case COMMUNE:
			return communeRFProcessor;
		case DROIT:
			return droitRFProcessor;
		case IMMEUBLE:
			return immeubleRFProcessor;
		case SURFACE_AU_SOL:
			return surfaceAuSolRFProcessor;
		default:
			throw new IllegalArgumentException("Type d'entité RF inconnue = [" + typeEntite + "]");
		}
	}

	@NotNull
	private List<Long> findIdsMutationsATraiter(long importId, EvenementRFMutation.TypeEntite typeEntite) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return evenementRFMutationDAO.findIds(importId, typeEntite, EtatEvenementRF.A_TRAITER, EtatEvenementRF.EN_ERREUR);
			}
		});
	}

	private void updateMutation(final long mutId, @Nullable final Exception e) {
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		t.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = getMutation(mutId);
				mutation.setEtat(EtatEvenementRF.EN_ERREUR);
				if (e != null) {
					mutation.setErrorMessage(ExceptionUtils.getStackTrace(e));
				}
			}
		});
	}
}
