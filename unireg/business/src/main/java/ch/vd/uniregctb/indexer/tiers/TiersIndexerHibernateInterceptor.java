package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

public class TiersIndexerHibernateInterceptor implements ModificationSubInterceptor, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private GlobalTiersIndexer indexer;
	private TransactionManager transactionManager;

	private final ThreadLocal<HashSet<Long>> modifiedEntities = new ThreadLocal<HashSet<Long>>();
	private final ThreadLocal<HashSet<Transaction>> registeredTransactions = new ThreadLocal<HashSet<Transaction>>();

	/**
	 * Cette méthode est appelé lorsque une entité hibernate est modifié/sauvé.
	 */
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		if (entity instanceof Tiers) {
			Tiers tiers = (Tiers) entity;
			addModifiedTiers(tiers);
		}
		else if (entity instanceof ForFiscal) {
			ForFiscal ff = (ForFiscal) entity;
			Tiers tiers = ff.getTiers();
			addModifiedTiers(tiers);
		}
		else if (entity instanceof AdresseTiers) {
			AdresseTiers adr = (AdresseTiers) entity;
			Tiers tiers = adr.getTiers();
			addModifiedTiers(tiers);
		}
		else if (entity instanceof RapportEntreTiers) {
			RapportEntreTiers ret = (RapportEntreTiers) entity;
			Tiers tiers1 = ret.getObjet();
			addModifiedTiers(tiers1);
			Tiers tiers2 = ret.getSujet();
			addModifiedTiers(tiers2);
		}

		return false; // encore tiers n'a été immédiatement modifié
	}

	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	/**
	 * Ajoute le tiers spécifié dans les liste des tiers qui seront indéxés après le flush.
	 *
	 * @param tiers le tiers en question.
	 */
	private void addModifiedTiers(Tiers tiers) {
		registerTxInterceptor();
		if (tiers != null) {
			getModifiedTiersIds().add(tiers.getNumero());
		}
	}

	private HashSet<Long> getModifiedTiersIds() {
		HashSet<Long> ent = modifiedEntities.get();
		if (ent == null) {
			ent = new HashSet<Long>();
			modifiedEntities.set(ent);
		}
		return ent;
	}

	protected void postCommit() {
		if (indexer.isOnTheFlyIndexation()) {
			indexModifiedTiers();
		}
		else {
			setDirtyModifiedTiers();
		}
	}

	/**
	 * Indexe ou réindexe les tiers modifiés
	 */
	private void indexModifiedTiers() {
		
		final HashSet<Long> ids = getModifiedTiersIds();
		if (ids.isEmpty()) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réindexation on-the-fly des tiers = " + Arrays.toString(ids.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate((PlatformTransactionManager) transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
				try {
					for (Long id : ids) {
						final Tiers tiers = (Tiers) session.get(Tiers.class, id);
						if (tiers == null) {
							// Le tiers peut être null si il y a eu un rollback, et donc pas de flush
							// Mais on n'est pas notifié en JTA...
							continue;
						}
						try {
							indexer.indexTiers(tiers, true/* Remove before reindexation */);
							if (tiers.isDirty()) {
								tiers.setIndexDirty(Boolean.FALSE); // il est plus dirty maintenant
							}
						}
						catch (Exception ee) {
							// Pour pas qu'un autre thread le reindex aussi
							tiers.setIndexDirty(true);

							final String message = "Reindexation du contribuable " + tiers.getId() + " impossible : " + ee.getMessage();
							LOGGER.error(message, ee);
						}
					}
					session.flush();
				}
				finally {
					session.close();
				}
				return null;
			}
		});

		ids.clear();
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réindexation on-the-fly des tiers terminée.");
		}
	}

	/**
	 * Met le flag 'dirty' à <i>vrai</i> sur tous les tiers modifiés en base de données.
	 */
	private void setDirtyModifiedTiers() {

		final HashSet<Long> ids = getModifiedTiersIds();
		if (ids.isEmpty()) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Passage à dirty des tiers = " + Arrays.toString(ids.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate((PlatformTransactionManager) transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
				try {
					for (Long id : ids) {
						final Tiers tiers = (Tiers) session.get(Tiers.class, id);
						if (tiers != null) {
							tiers.setIndexDirty(true);
						}
					}
					session.flush();
				}
				finally {
					session.close();
				}
				return null;
			}
		});

		ids.clear();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Passage à dirty des tiers terminée.");
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

	private void registerTxInterceptor() {
		try {
			Transaction transaction = transactionManager.getTransaction();
			if (!getRegisteredTransactionsSet().contains(transaction)) {
				transaction.registerSynchronization(new TxInterceptor(transaction));
			}
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'enregistrer l'intercepteur de transaction, les tiers modifiés ne seront pas réindéxés.", e);
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
			// rien de spécial à faire
		}

		public void afterCompletion(int status) {

			Assert.isTrue(status == Status.STATUS_COMMITTED || status == Status.STATUS_ROLLEDBACK); // il me semble que tous les autres status n'ont pas de sens ici

			// on se désenregistre soi-même
			unregisterTxInterceptor(transaction);

			if (status == Status.STATUS_COMMITTED) {
				postCommit();
			}
		}
	}
	
	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
