package ch.vd.uniregctb.common;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.AuthenticationInterface;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplateWithResults;
import ch.vd.shared.batchtemplate.StatusManager;

/**
 * Classe utilitaire qui reprend la fonctionnalité du {@link ch.vd.uniregctb.common.BatchTransactionTemplateWithResults} et ajoute celle de traiter les lots avec <i>n</i> threads en parallèle.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ParallelBatchTransactionTemplateWithResult<E, R extends BatchResults<E, R>> extends ParallelBatchTransactionTemplateWithResults<E, R>{

	private static final TransactionTemplateFactory TRANSACTION_TEMPLATE_FACTORY = new TransactionTemplateFactory();

	public ParallelBatchTransactionTemplateWithResult(List<E> elements, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager,
	                                                  @Nullable StatusManager statusManager, @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, batchSize, nbThreads, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager, authenticationInterface);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
