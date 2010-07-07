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
	private final ThreadLocal<HashSet<Transaction>> registeredTransactions = new ThreadLocal<HashSet<Transaction>>();
	private final ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();
	private final List<ModificationSubInterceptor> subInterceptors = new ArrayList<ModificationSubInterceptor>();

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void register(ModificationSubInterceptor sub) {
		subInterceptors.add(sub);
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

		for (ModificationSubInterceptor s : subInterceptors) {
			modified = s.onChange(entity, id, currentState, previousState, propertyNames, types) || modified;
		}

		return modified;
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
		HashSet<Transaction> set = registeredTransactions.get();
		if (set == null) {
			set = new HashSet<Transaction>();
			registeredTransactions.set(set);
		}
		return set;
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

		private Transaction transaction;

		public TxInterceptor(Transaction transaction) {
			this.transaction = transaction;
		}

		public void beforeCompletion() {
			preTransactionCommit();
		}

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
