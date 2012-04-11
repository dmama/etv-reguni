package ch.vd.uniregctb.hibernate.interceptor;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.type.Type;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Intercepteur hibernate qui détecte lorsqu'une entité hibernate (HibernateEntity) est ajoutée ou modifiée dans la base de données, et
 * notifie du changements une liste de sous-intercepteurs. Cette intercepteur notifie également des phases de commit des transactions.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ModificationInterceptor extends AbstractLinkedInterceptor {

	private static final Logger LOGGER = Logger.getLogger(ModificationInterceptor.class);

	private TransactionManager transactionManager;
	private final ThreadLocal<HashSet<Transaction>> registeredTransactions = new ThreadLocal<HashSet<Transaction>>() {
		@Override
		protected HashSet<Transaction> initialValue() {
			return new HashSet<Transaction>();
		}
	};
	private final ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();
	private final List<ModificationSubInterceptor> subInterceptors = new ArrayList<ModificationSubInterceptor>();

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void register(ModificationSubInterceptor sub) {
		subInterceptors.add(sub);
	}

	public void unregister(ModificationSubInterceptor sub) {
		subInterceptors.remove(sub);
	}

	/**
	 * Désactive ou réactive l'intercepteur pour le thread courant.
	 *
	 * @param value l'intercepteur doit être actif ou non
	 */
	public void setEnabledForThread(boolean value) {
		disabled.set(!value);
	}

	/**
	 * Détecte si l'entité spécifiée a réellement changé et retourne <b>vrai</b> si c'est le cas.
	 */
	private static boolean entityChanged(String[] propertyNames, Object[] currentState, Object[] previousState) {

		boolean changed = false;
		if (previousState == null) { // msi: on a eu ce cas lors de l'envoi des DIs...
			changed = true;
		}
		else {
			for (int i = 0; i < currentState.length; i++) {

				// String name = propertyNames[i];
				Object c = currentState[i];
				Object p = previousState[i];

				if ((c != null && p == null) || (c == null && p != null)) {
					changed = true;
					break;
				}
				else if (c != null && p != null) {
					if (c instanceof AbstractPersistentCollection) {
						AbstractPersistentCollection cc = (AbstractPersistentCollection) c;
						changed = cc.isDirty();
					}
					else if (!c.equals(p)) {
						changed = true;
					}
					if (changed) {
						break;
					}
				}
			}
		}

		return changed;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		registerTxInterceptor();

		boolean modified = false;

		if (entity instanceof HibernateEntity && entityChanged(propertyNames, currentState, previousState)) {
			modified = onChange((HibernateEntity) entity, id, currentState, previousState, propertyNames, types);
		}

		return modified;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] currentState, String[] propertyNames, Type[] types)
			throws CallbackException {

		registerTxInterceptor();

		boolean modified = false;

		if (entity instanceof HibernateEntity) {
			modified = onChange((HibernateEntity) entity, id, currentState, null, propertyNames, types);
		}

		return modified;
	}

	@Override
	public void postFlush(Iterator<?> entities) throws CallbackException {

		registerTxInterceptor();
		
		for (ModificationSubInterceptor s : subInterceptors) {
			s.postFlush();
		}
	}

	private boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {

		boolean modified = false;
		boolean isAnnulation = detectAnnulation(currentState, previousState, propertyNames);

		for (ModificationSubInterceptor s : subInterceptors) {
			modified = s.onChange(entity, id, currentState, previousState, propertyNames, types, isAnnulation) || modified;
		}

		return modified;
	}

	/**
	 * Détecte si l'entité vient d'être annulée
	 *
	 * @param currentState  l'état courant de l'entité
	 * @param previousState l'état précédant de l'entité
	 * @param propertyNames les noms des propriétés des états
	 * @return <b>vrai</b> si l'entité vient d'être annulée; <b>faux</b> autrement.
	 */
	private static boolean detectAnnulation(Object[] currentState, Object[] previousState, String[] propertyNames) {
		if (previousState == null) {
			// si l'objet n'existait pas, il n'y a pas de transition non-annulé -> annulé possible.
			return false;
		}
		int index = -1;
		for (int i = 0, propertyNamesLength = propertyNames.length; i < propertyNamesLength; i++) {
			final String name = propertyNames[i];
			if ("annulationDate".equals(name)) {
				index = i;
				break;
			}
		}
		// si la date d'annulation était nulle et qu'elle ne l'est plus, alors on affaire à une annulation
		return index >= 0 && previousState[index] == null && currentState[index] != null;
	}

	private void preTransactionCommit() {
		for (ModificationSubInterceptor s : subInterceptors) {
			s.preTransactionCommit();
		}
	}

	private void postTransactionCommit() {
		for (ModificationSubInterceptor s : subInterceptors) {
			s.postTransactionCommit();
		}
	}

	private void postTransactionRollback() {
		for (ModificationSubInterceptor s : subInterceptors) {
			s.postTransactionRollback();
		}
	}

	private HashSet<Transaction> getRegisteredTransactionsSet() {
		return registeredTransactions.get();
	}

	/**
	 * Enregistre un callback sur la transaction de manière à être notifier du commit ou du rollback
	 */
	private void registerTxInterceptor() {
		if (disabled.get() != null && disabled.get()) {
			return;
		}
		try {
			final Transaction transaction = transactionManager.getTransaction();
			final HashSet<Transaction> set = getRegisteredTransactionsSet();
			if (!set.contains(transaction)) {
				transaction.registerSynchronization(new TxInterceptor(transaction));
				set.add(transaction);
			}
		}
		catch (RollbackException e) {
			LOGGER.debug("Impossible d'engistrer l'intercepteur de transaction car la transaction est marquée rollback-only. Tant pis, on l'ignore.", e);
		}
		catch (Exception e) {
			final String message = "Impossible d'enregistrer l'intercepteur de transaction : y a-t-il une transaction d'ouverte ?";
			LOGGER.error(message, e);
			throw new RuntimeException(message, e);
		}
	}

	protected void unregisterTxInterceptor(Transaction transaction) {
		registeredTransactions.get().remove(transaction);
	}

	private class TxInterceptor implements Synchronization {

		private final Transaction transaction;

		public TxInterceptor(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public void beforeCompletion() {
			preTransactionCommit();
		}

		@Override
		public void afterCompletion(int status) {

			Assert.isTrue(status == Status.STATUS_COMMITTED || status == Status.STATUS_ROLLEDBACK); // il me semble que tous les autres status n'ont pas de sens ici

			// on se désenregistre soi-même
			unregisterTxInterceptor(transaction);

			if (status == Status.STATUS_COMMITTED) {
				postTransactionCommit();
			}
			else {
				postTransactionRollback();
			}
		}
	}
}
