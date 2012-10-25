package ch.vd.uniregctb.common;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Classe qui fonctionne comme le {@link TransactionTemplate} de Spring, mais qui utilise un callback avec des exceptions checkées.
 */
public class CheckedTransactionTemplate {

	private static class InternalException extends RuntimeException {

		private final Exception exception;

		private InternalException(Exception exception) {
			this.exception = exception;
		}

		public Exception getException() {
			return exception;
		}

	}

	private TransactionTemplate template;

	public CheckedTransactionTemplate(PlatformTransactionManager transactionManager) {
		this.template = new TransactionTemplate(transactionManager);
	}

	public void setPropagationBehavior(int behavior) {
		template.setPropagationBehavior(behavior);
	}

	/**
	 * Exécute le callback spécifié à l'intérieur d'une transaction (même comportement que le {@link TransactionTemplate} de Spring, mais qui supporte des exceptions checkées.
	 *
	 * @param action le callback d'action à effectuer
	 * @param <T>    le type de retour du callback
	 * @return la valeur retournée par le callback
	 * @throws Exception l'exception levée par le callback (dans ce cas, la transaction est rollée-back).
	 */
	public <T> T execute(final CheckedTransactionCallback<T> action) throws Exception {
		try {
			return template.execute(new TransactionCallback<T>() {
				@Override
				public T doInTransaction(TransactionStatus status) {
					try {
						return action.doInTransaction(status);
					}
					catch (Exception e) {
						throw new InternalException(e);
					}
				}
			});
		}
		catch (InternalException e) {
			throw e.getException();
		}
	}
}
