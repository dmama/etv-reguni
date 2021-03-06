package ch.vd.unireg.common;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.AuthenticationInterface;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.Interruptible;

public class ParallelBatchTransactionTemplate<T> extends ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplate<T> {

	public ParallelBatchTransactionTemplate(List<T> elements, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible,
	                                        @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	public ParallelBatchTransactionTemplate(Iterator<T> iterator, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible,
	                                        @Nullable AuthenticationInterface authenticationInterface) {
		super(iterator, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	public ParallelBatchTransactionTemplate(Iterator<T> iterator, int totalSize, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible,
	                                        @Nullable AuthenticationInterface authenticationInterface) {
		super(iterator, totalSize, batchSize, nbThreads, behavior, transactionManager, interruptible, authenticationInterface);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
