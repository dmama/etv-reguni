package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.BatchIterator;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.Interruptible;

public class BatchTransactionTemplateWithResults<E, R extends BatchResults<E, R>> extends ch.vd.shared.batchtemplate.BatchTransactionTemplateWithResults<E, R> {

	/**
	 * @param iterator           un itérateur qui retourne les éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param interruptible      un callback pour savoir si le batch a reçu une demande d'interruption (peut être nul)
	 */
	public BatchTransactionTemplateWithResults(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, batchSize, behavior, transactionManager, interruptible);
	}

	/**
	 * @param iterator           un itérateur qui retourne les éléments à processer
	 * @param totalSize          le nombre d'éléments à sortir de l'itérateur (pour pouvoir gérer une progression)
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param interruptible      un callback pour savoir si le batch a reçu une demande d'interruption (peut être nul)
	 */
	public BatchTransactionTemplateWithResults(Iterator<E> iterator, int totalSize, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, totalSize, batchSize, behavior, transactionManager, interruptible);
	}

	/**
	 * @param list               la liste des éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param interruptible      un callback pour savoir si le batch a reçu une demande d'interruption (peut être nul)
	 */
	public BatchTransactionTemplateWithResults(Collection<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(list, batchSize, behavior, transactionManager, interruptible);
	}

	public BatchTransactionTemplateWithResults(BatchIterator<E> iterator, Behavior behavior, PlatformTransactionManager transactionManager, @Nullable Interruptible interruptible) {
		super(iterator, behavior, transactionManager, interruptible);
	}

	@Override
	protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
		return true;
	}
}
