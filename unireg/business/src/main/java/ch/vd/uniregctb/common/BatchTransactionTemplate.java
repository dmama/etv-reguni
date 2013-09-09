package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.BatchIterator;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;

public class BatchTransactionTemplate<E> extends ch.vd.shared.batchtemplate.BatchTransactionTemplate<E> {

	private static final TransactionTemplateFactory TRANSACTION_TEMPLATE_FACTORY = new TransactionTemplateFactory();

	public BatchTransactionTemplate(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(iterator, batchSize, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	public BatchTransactionTemplate(Collection<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(list, batchSize, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	public BatchTransactionTemplate(BatchIterator<E> iterator, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(iterator, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
