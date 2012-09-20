package ch.vd.uniregctb.indexer.tiers;

import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

public class TiersIndexerHibernateInterceptor implements ModificationSubInterceptor, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private GlobalTiersIndexer indexer;
	private TransactionManager transactionManager;
	private Dialect dialect;

	private final ThreadLocal<HashSet<Long>> modifiedEntities = new ThreadLocal<HashSet<Long>>() {
		@Override
		protected HashSet<Long> initialValue() {
			return new HashSet<Long>();
		}
	};

	/**
	 * Cette méthode est appelé lorsque une entité hibernate est modifié/sauvé.
	 */
	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
	                        Type[] types, boolean isAnnulation) throws CallbackException {

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
			Long tiersId1 = ret.getObjetId();
			addModifiedTiers(tiersId1);
			Long tiersId2 = ret.getSujetId();
			addModifiedTiers(tiersId2);
		}

		return false; // aucun tiers n'a été immédiatement modifié
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {
		if (indexer.isOnTheFlyIndexation()) {
			indexModifiedTiers();
		}
		else {
			setDirtyModifiedTiers();
		}
	}

	@Override
	public void postTransactionRollback() {
		getModifiedTiersIds().clear();
	}

	/**
	 * Ajoute le tiers spécifié dans les liste des tiers qui seront indéxés après le flush.
	 *
	 * @param tiers le tiers en question.
	 */
	private void addModifiedTiers(Tiers tiers) {
		if (tiers != null) {
			getModifiedTiersIds().add(tiers.getNumero());
		}
	}

	/**
	 * Ajoute le tiers spécifié dans les liste des tiers qui seront indéxés après le flush.
	 *
	 * @param tiersId l'id du tiers en question.
	 */
	private void addModifiedTiers(Long tiersId) {
		if (tiersId != null) {
			getModifiedTiersIds().add(tiersId);
		}
	}

	private HashSet<Long> getModifiedTiersIds() {
		return modifiedEntities.get();
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
			LOGGER.debug("Demande de réindexation on-the-fly des tiers = " + Arrays.toString(ids.toArray()));
		}

		// Demande la ré-indexation des tiers modifiés
		indexer.schedule(ids);

		ids.clear();
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

		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
				try {
					final SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(true) + " where NUMERO in (:ids)");

					final BatchIterator<Long> batchIterator = new StandardBatchIterator<Long>(ids, 500);    // n'oublions pas qu'Oracle ne supporte pas plus de 1000 objets dans un IN
					while (batchIterator.hasNext()) {
						final Collection<Long> subSet = batchIterator.next();
						if (subSet != null && !subSet.isEmpty()) {
							query.setParameterList("ids", subSet);
							query.executeUpdate();
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
