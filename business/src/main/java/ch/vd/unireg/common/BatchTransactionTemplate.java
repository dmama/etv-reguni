package ch.vd.unireg.common;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.BatchIterator;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.Interruptible;

public class BatchTransactionTemplate<E> extends ch.vd.shared.batchtemplate.BatchTransactionTemplate<E> {

	public BatchTransactionTemplate(Iterator<E> iterator, int totalSize, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, totalSize, batchSize, behavior, transactionManager, interruptible);
	}

	public BatchTransactionTemplate(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, batchSize, behavior, transactionManager, interruptible);
	}

	public BatchTransactionTemplate(Collection<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(list, batchSize, behavior, transactionManager, interruptible);
	}

	public BatchTransactionTemplate(BatchIterator<E> iterator, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, behavior, transactionManager, interruptible);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
