package ch.vd.uniregctb.common;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.AuthenticationInterface;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.Interruptible;

/**
 * Classe utilitaire qui reprend la fonctionnalité du {@link ch.vd.uniregctb.common.BatchTransactionTemplateWithResults} et ajoute celle de traiter les lots avec <i>n</i> threads en parallèle.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ParallelBatchTransactionTemplateWithResults<E, R extends BatchResults<E, R>> extends ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplateWithResults<E, R> {

	public ParallelBatchTransactionTemplateWithResults(List<E> elements, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager,
	                                                   @Nullable Interruptible interruptible, @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	public ParallelBatchTransactionTemplateWithResults(Iterator<E> elements, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager,
	                                                   @Nullable Interruptible interruptible, @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	public ParallelBatchTransactionTemplateWithResults(Iterator<E> elements, int totalSize, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager,
	                                                   @Nullable Interruptible interruptible, @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, totalSize, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
