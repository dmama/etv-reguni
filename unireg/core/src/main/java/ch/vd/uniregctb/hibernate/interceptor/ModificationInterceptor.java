package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.hibernate.CallbackException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.type.Type;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LockHelper;
import ch.vd.uniregctb.common.ThreadSwitch;

/**
 * Intercepteur hibernate qui détecte lorsqu'une entité hibernate (HibernateEntity) est ajoutée ou modifiée dans la base de données, et
 * notifie du changements une liste de sous-intercepteurs. Cette intercepteur notifie également des phases de commit des transactions.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ModificationInterceptor extends AbstractLinkedInterceptor {

	private final ThreadSwitch activationSwitch = new ThreadSwitch(true);
	private final LockHelper lockHelper = new LockHelper();
	private final ThreadLocal<TransactionInterceptor> transactions = ThreadLocal.withInitial(TransactionInterceptor::new);
	private final List<ModificationSubInterceptor> subInterceptors = new ArrayList<>();

	public void register(ModificationSubInterceptor sub) {
		lockHelper.doInWriteLock(() -> subInterceptors.add(sub));
	}

	public void unregister(ModificationSubInterceptor sub) {
		lockHelper.doInWriteLock(() -> subInterceptors.remove(sub));
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
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {

		registerSynchronisationOnCurrentTransaction();

		boolean modified = false;

		if (entity instanceof HibernateEntity && entityChanged(propertyNames, currentState, previousState)) {
			modified = onChange((HibernateEntity) entity, id, currentState, previousState, propertyNames, types);
		}

		return modified;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] currentState, String[] propertyNames, Type[] types) throws CallbackException {

		registerSynchronisationOnCurrentTransaction();

		boolean modified = false;

		if (entity instanceof HibernateEntity) {
			modified = onChange((HibernateEntity) entity, id, currentState, null, propertyNames, types);
		}

		return modified;
	}

	@Override
	public void postFlush(Iterator<?> entities) throws CallbackException {
		registerSynchronisationOnCurrentTransaction();
		lockHelper.doInReadLock(() -> subInterceptors.forEach(ModificationSubInterceptor::postFlush));
	}

	private boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {

		final Mutable<Boolean> modified = new MutableBoolean(false);
		final boolean isAnnulation = detectAnnulation(currentState, previousState, propertyNames);

		lockHelper.doInReadLock(() -> {
			for (ModificationSubInterceptor s : subInterceptors) {
				modified.setValue(s.onChange(entity, id, currentState, previousState, propertyNames, types, isAnnulation) || modified.getValue());
			}
		});

		return modified.getValue();
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
		lockHelper.doInReadLock(() -> subInterceptors.forEach(ModificationSubInterceptor::preTransactionCommit));
	}

	private void postTransactionCommit() {
		lockHelper.doInReadLock(() -> subInterceptors.forEach(ModificationSubInterceptor::postTransactionCommit));
	}

	private void postTransactionRollback() {
		lockHelper.doInReadLock(() -> subInterceptors.forEach(ModificationSubInterceptor::postTransactionRollback));
	}

	private void registerSynchronisationOnCurrentTransaction() {
		// si l'objet n'existe pas encore pour la transaction courante, on le crée et on l'enregistre dans la transaction
		// (s'il existe déjà, on ne fait rien...)
		transactions.get();
	}

	private void unregisterSynchronisation() {
		// Il est particulièrement important d'effacer la donnée dans le ThreadLocal afin que la déréférence suivante
		// de la donnée force une nouvelle instanciation de l'objet et donc son inscription dans la transaction courante
		// (y compris le nettoyage final)
		transactions.remove();
	}

	/**
	 * Objet interne qui s'enregistre dans la transaction courante au moment de son instanciation et fait en sorte
	 * que les callbacks des sub-intercepteurs soient appelés au bon moment vis-à-vis de cette transaction.
	 * <br/>
	 * Une fois la transaction terminée, l'instance se retire d'elle même pour laisser la place pour la prochaine transaction
	 * (au travers du {@link ThreadLocal} {@link #transactions})
	 */
	private class TransactionInterceptor {

		public TransactionInterceptor() {

			if (!TransactionSynchronizationManager.isActualTransactionActive()) {
				throw new IllegalStateException("Une transaction est sensée être en cours...");
			}

			final TransactionSynchronization synchronization;

			// ce constructeur est appelé dans le thread de la transaction, et il peut-être sans
			// effet si tel est le souhait du développeur...
			if (isEnabledForThread()) {

				// synchronisation qui appelle les callbacks
				synchronization = new TransactionSynchronizationAdapter() {
					@Override
					public void beforeCommit(boolean readOnly) {
						super.beforeCommit(readOnly);
						preTransactionCommit();
					}

					@Override
					public void afterCompletion(int status) {
						try {
							super.afterCompletion(status);
						}
						finally {
							// transaction committée ou annulée, il faut tout nettoyer...
							// (et tant qu'à faire, avant les appels aux postTransaction... au cas où ceux-ci
							// ré-ouvriraient une transaction...)
							unregisterSynchronisation();
						}

						switch (status) {
						case TransactionSynchronization.STATUS_COMMITTED:
							postTransactionCommit();
							break;
						case TransactionSynchronization.STATUS_ROLLED_BACK:
							postTransactionRollback();
							break;
						default:
							throw new IllegalStateException("Transaction ni committée, ni annulée...");
						}
					}
				};
			}
			else {

				// synchronisation qui ne fait rien (si ce n'est se dés-enregistrer à la fin de la transaction...)
				synchronization = new TransactionSynchronizationAdapter() {
					@Override
					public void afterCompletion(int status) {
						try {
							super.afterCompletion(status);
						}
						finally {
							// transaction committée ou annulée, il faut tout nettoyer...
							unregisterSynchronisation();
						}
					}
				};
			}

			// on lie la synchronisation à la transaction courante...
			TransactionSynchronizationManager.registerSynchronization(synchronization);
		}
	}
}
