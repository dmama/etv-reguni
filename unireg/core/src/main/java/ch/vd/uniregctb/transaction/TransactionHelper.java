package ch.vd.uniregctb.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionHelper {

	private final PlatformTransactionManager transactionManager;

	public TransactionHelper(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Lance le traitement du callback dans une transaction indépendante
	 * @param readonly <code>true</code> si la transaction doit être ouverte en read-only
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	public <T> T doInTransaction(boolean readonly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(readonly);
		return template.execute(callback);
	}

	/**
	 * Lance le traitement du callback dans une transaction indépendante
	 * @param readonly <code>true</code> si la transaction doit être ouverte en read-only
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	public void doInTransaction(boolean readonly, final TransactionCallbackWithoutResult callback) {
		doInTransaction(readonly, new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				callback.doInTransaction(status);
				return null;
			}
		});
	}

	/**
	 * Interface de callback d'un traitement qui renvoie un résultat et peut lancer une exception
	 * @param <T> type du résultat renvoyé
	 * @param <E> type de l'exception lancée
	 */
	public interface ExceptionThrowingCallback<T, E extends Exception> {
		T execute(TransactionStatus status) throws E;
	}

	/**
	 * Interface de callback d'un traitement qui ne renvoie aucun résultat mais peut lancer une exception
	 * @param <E> type de l'exception lancée
	 */
	public interface ExceptionThrowingCallbackWithoutResult<E extends Exception> {
		void execute(TransactionStatus status) throws E;
	}

	/**
	 * Classe interne de wrapping qui permet de faire passer une exception checkée
	 * au travers d'une couche qui ne le permet pas
	 */
	private static class WrappingException extends RuntimeException {
		public WrappingException(Exception cause) {
			super(cause);
		}

		@Override
		public synchronized Exception getCause() {
			return (Exception) super.getCause();
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction indépendante
	 * @param readonly <code>true</code> si la transaction doit être ouverte en read-only
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 * @param <T> type du résultat renvoyé par le callback
	 * @param <E> type de l'exeption lancée par le callback
	 * @return le résultat renvoyé par le callback
	 * @throws E en cas de souci
	 */
	public <T, E extends Exception> T doInTransactionWithException(boolean readonly, final ExceptionThrowingCallback<T, E> callback) throws E {
		try {
			return doInTransaction(readonly, new TransactionCallback<T>() {
				@Override
				public T doInTransaction(TransactionStatus status) {
					try {
						return callback.execute(status);
					}
					catch (RuntimeException e) {
						throw e;
					}
					catch (Exception e) {
						// d'après la signature du callback, cela ne peut être qu'une exception de classe E
						throw new WrappingException(e);
					}
				}
			});
		}
		catch (WrappingException e) {
			//noinspection unchecked
			throw (E) e.getCause();
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction indépendante
	 * @param readonly <code>true</code> si la transaction doit être ouverte en read-only
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 * @param <E> type de l'exeption lancée par le callback
	 * @throws E en cas de souci
	 */
	public <E extends Exception> void doInTransactionWithException(boolean readonly, final ExceptionThrowingCallbackWithoutResult<E> callback) throws E {
		doInTransactionWithException(readonly, new ExceptionThrowingCallback<Object, E>() {
			@Override
			public Object execute(TransactionStatus status) throws E {
				callback.execute(status);
				return null;
			}
		});
	}
}
