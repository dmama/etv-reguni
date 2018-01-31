package ch.vd.uniregctb.common;

import org.springframework.transaction.TransactionStatus;

/**
 * Callback utilisé avec la classe {@link CheckedTransactionTemplate}.
 *
 * @param <T> le type de retour du callback
 */
public interface CheckedTransactionCallback<T> {
	T doInTransaction(TransactionStatus status) throws Exception;
}
