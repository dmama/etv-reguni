package ch.vd.uniregctb.registrefoncier;

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
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessor;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Processeur responsable de traiter les mutations du registre foncier.
 */
public class DataRFMutationsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataRFMutationsProcessor.class);

	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final MutationRFProcessor immeubleProcessor;

	public DataRFMutationsProcessor(@NotNull EvenementRFMutationDAO evenementRFMutationDAO,
	                                @NotNull MutationRFProcessor immeubleRFProcessor,
	                                @NotNull PlatformTransactionManager transactionManager) {
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.immeubleProcessor = immeubleRFProcessor;
	}

	/**
	 * Traite tous les mutations à l'état A_TRAITER de l'import spécifié
	 *
	 * @param importId l'id d'un import du registre foncier
	 */
	public void processImport(long importId) {

		final List<Long> ids = findIdsMutationsATraiter(importId);

		// TODO (msi) générer un rapport
		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<>(ids, 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(new BatchCallback<Long>() {

			private List<Long> mutationsIds;

			@Override
			public boolean doInTransaction(List<Long> mutationsIds) throws Exception {
				this.mutationsIds = mutationsIds;
				mutationsIds.stream()
						.map(id -> getMutation(id))
						.forEach(mut -> processMutation(mut));
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = CollectionsUtils.getFirst(mutationsIds);
					LOGGER.warn("Erreur pendant le traitement de la mutation n°" + mutId, e);
					if (mutId != null) {
						updateMutation(mutId, e);
					}
				}
			}
		}, null);
	}

	private void processMutation(@NotNull EvenementRFMutation mut) {
		final MutationRFProcessor proc = getProcessor(mut);
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
	private MutationRFProcessor getProcessor(@NotNull EvenementRFMutation mut) {
		switch (mut.getTypeEntite()) {
		case AYANT_DROIT:
			// TODO (msi)
			throw new NotImplementedException();
		case BATIMENT:
			// TODO (msi)
			throw new NotImplementedException();
		case DROIT:
			// TODO (msi)
			throw new NotImplementedException();
		case IMMEUBLE:
			return immeubleProcessor;
		case SURFACE:
			// TODO (msi)
			throw new NotImplementedException();
		default:
			throw new IllegalArgumentException("Type d'entité RF inconnue = [" + mut.getTypeEntite() + "]");
		}
	}

	@NotNull
	private List<Long> findIdsMutationsATraiter(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return evenementRFMutationDAO.findIds(importId, EtatEvenementRF.A_TRAITER);
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
