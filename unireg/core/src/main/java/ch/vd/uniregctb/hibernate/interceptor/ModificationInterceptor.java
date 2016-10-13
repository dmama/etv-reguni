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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.CallbackException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.ThreadSwitch;

/**
 * Intercepteur hibernate qui détecte lorsqu'une entité hibernate (HibernateEntity) est ajoutée ou modifiée dans la base de données, et
 * notifie du changements une liste de sous-intercepteurs. Cette intercepteur notifie également des phases de commit des transactions.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ModificationInterceptor extends AbstractLinkedInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModificationInterceptor.class);

	private final ThreadSwitch activationSwitch = new ThreadSwitch(true);
	private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	private TransactionManager transactionManager;
	private final ThreadLocal<HashSet<Transaction>> registeredTransactions = ThreadLocal.withInitial(HashSet::new);
	private final List<ModificationSubInterceptor> subInterceptors = new ArrayList<>();

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void register(ModificationSubInterceptor sub) {
		final Lock lock = rwlock.writeLock();
		lock.lock();
		try {
			subInterceptors.add(sub);
		}
		finally {
			lock.unlock();
		}
	}

	public void unregister(ModificationSubInterceptor sub) {
		final Lock lock = rwlock.writeLock();
		lock.lock();
		try {
			subInterceptors.remove(sub);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Désactive ou réactive l'intercepteur pour le thread courant.
	 *
	 * @param value l'intercepteur doit être actif ou non
	 */
	public void setEnabledForThread(boolean value) {
		activationSwitch.setEnabled(value);
	}

	public boolean isEnabledForThread() {
		return activationSwitch.isEnabled();
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
				else if (c != null) {
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

		final Lock lock = rwlock.readLock();
		lock.lock();
		try {
			for (ModificationSubInterceptor s : subInterceptors) {
				s.postFlush();
			}
		}
		finally {
			lock.unlock();
		}
	}

	private boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {

		boolean modified = false;
		boolean isAnnulation = detectAnnulation(currentState, previousState, propertyNames);

		final Lock lock = rwlock.readLock();
		lock.lock();
		try {
			for (ModificationSubInterceptor s : subInterceptors) {
				modified = s.onChange(entity, id, currentState, previousState, propertyNames, types, isAnnulation) || modified;
			}
		}
		finally {
			lock.unlock();
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
		final Lock lock = rwlock.readLock();
		lock.lock();
		try {
			for (ModificationSubInterceptor s : subInterceptors) {
				s.preTransactionCommit();
			}
		}
		finally {
			lock.unlock();
		}
	}

	private void postTransactionCommit() {
		final Lock lock = rwlock.readLock();
		lock.lock();
		try {
			for (ModificationSubInterceptor s : subInterceptors) {
				s.postTransactionCommit();
			}
		}
		finally {
			lock.unlock();
		}
	}

	private void postTransactionRollback() {
		final Lock lock = rwlock.readLock();
		lock.lock();
		try {
			for (ModificationSubInterceptor s : subInterceptors) {
				s.postTransactionRollback();
			}
		}
		finally {
			lock.unlock();
		}
	}

	private HashSet<Transaction> getRegisteredTransactionsSet() {
		return registeredTransactions.get();
	}

	/**
	 * Enregistre un callback sur la transaction de manière à être notifier du commit ou du rollback
	 */
	private void registerTxInterceptor() {
		if (!isEnabledForThread()) {
			return;
		}
		try {
			final Transaction transaction = transactionManager.getTransaction();
			if (transaction == null) {
				throw new IllegalArgumentException("Il n'y a pas de transaction ouverte sur le transaction manager = " + transactionManager);
			}
			final HashSet<Transaction> set = getRegisteredTransactionsSet();
			if (!set.contains(transaction)) {
				transaction.registerSynchronization(new TxInterceptor(transaction));
				set.add(transaction);
			}
		}
		catch (RollbackException e) {
			LOGGER.debug("Impossible d'engistrer l'intercepteur de transaction car la transaction est marquée rollback-only. Tant pis, on l'ignore.", e);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (Exception e) {
			final String message = "Impossible d'enregistrer l'intercepteur de transaction";
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
