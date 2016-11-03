package ch.vd.uniregctb.common;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.AuthenticationInterface;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;

public class ParallelBatchTransactionTemplate<T> extends ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplate<T> {

	private static final ch.vd.uniregctb.common.TransactionTemplateFactory TRANSACTION_TEMPLATE_FACTORY = new ch.vd.uniregctb.common.TransactionTemplateFactory();

	public ParallelBatchTransactionTemplate(List<T> elements, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager,
	                                        @Nullable AuthenticationInterface authenticationInterface) {
		super(elements, batchSize, nbThreads, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager, authenticationInterface);
	}

	public ParallelBatchTransactionTemplate(Iterator<T> iterator, int batchSize, int nbThreads, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager,
	                                        @Nullable AuthenticationInterface authenticationInterface) {
		super(iterator, batchSize, nbThreads, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager, authenticationInterface);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
