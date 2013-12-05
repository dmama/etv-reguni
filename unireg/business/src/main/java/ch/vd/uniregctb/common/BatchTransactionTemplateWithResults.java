package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.BatchIterator;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;

public class BatchTransactionTemplateWithResults<E, R extends BatchResults<E, R>> extends ch.vd.shared.batchtemplate.BatchTransactionTemplateWithResults<E, R> {

	private static final TransactionTemplateFactory TRANSACTION_TEMPLATE_FACTORY = new TransactionTemplateFactory();

	/**
	 * @param iterator           un itérateur qui retourne les éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager (peut être nul)
	 */
	public BatchTransactionTemplateWithResults(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(iterator, batchSize, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	/**
	 * @param list               la liste des éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager (peut être nul)
	 */
	public BatchTransactionTemplateWithResults(Collection<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(list, batchSize, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	public BatchTransactionTemplateWithResults(BatchIterator<E> iterator, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable StatusManager statusManager) {
		super(iterator, behavior, transactionManager, TRANSACTION_TEMPLATE_FACTORY, statusManager);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
