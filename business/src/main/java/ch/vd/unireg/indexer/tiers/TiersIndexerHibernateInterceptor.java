package ch.vd.unireg.indexer.tiers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.EntityKey;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.StackedThreadLocal;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.unireg.tiers.Tiers;

public class TiersIndexerHibernateInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersIndexerHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private GlobalTiersIndexer indexer;
	private PlatformTransactionManager transactionManager;
	private Dialect dialect;
	private HibernateTemplate hibernateTemplate;

	private final StackedThreadLocal<Set<Long>> modifiedEntities = new StackedThreadLocal<>(HashSet::new);

	/**
	 * Cette méthode est appelé lorsque une entité hibernate est modifié/sauvé.
	 */
	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
	                        Type[] types, boolean isAnnulation) throws CallbackException {

		if (entity instanceof Tiers) {
			final Tiers tiers = (Tiers) entity;
			addModifiedTiers(tiers);
		}
		else if (entity instanceof LinkedEntity) {
			final LinkedEntity linked = (LinkedEntity) entity;
			final List<?> linkedEntities = linked.getLinkedEntities(new LinkedEntityContext(LinkedEntityPhase.INDEXATION, hibernateTemplate), true);
			if (linkedEntities != null && !linkedEntities.isEmpty()) {
				for (Object linkedEntity : linkedEntities) {
					if (linkedEntity instanceof Tiers) {
						addModifiedTiers((Tiers) linkedEntity);
					}
					else if (linkedEntity instanceof EntityKey) {
						final EntityKey key = (EntityKey) linkedEntity;
						if (Tiers.class.isAssignableFrom(key.getClazz())) {
							addModifiedTiers((Long) key.getId());           // la clé des Tiers est un Long
						}
					}
				}
			}
		}

		return false; // aucun tiers n'a été immédiatement modifié
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void suspendTransaction() {
		modifiedEntities.pushState();
	}

	@Override
	public void resumeTransaction() {
		modifiedEntities.popState();
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {
		if (indexer.onTheFlyIndexationSwitch().isEnabled()) {
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

	private Set<Long> getModifiedTiersIds() {
		return modifiedEntities.get();
	}

	/**
	 * Indexe ou réindexe les tiers modifiés
	 */
	private void indexModifiedTiers() {
		
		final Set<Long> ids = getModifiedTiersIds();
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

		final Set<Long> ids = getModifiedTiersIds();
		if (ids.isEmpty()) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Passage à dirty des tiers = " + Arrays.toString(ids.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final boolean enabled = parent.isEnabledForThread();
		parent.setEnabledForThread(false);
		try {
			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final Session session = sessionFactory.openSession();
					try {
						final SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(true) + " where NUMERO in (:ids)");

						final BatchIterator<Long> batchIterator = new StandardBatchIterator<>(ids, 500);    // n'oublions pas qu'Oracle ne supporte pas plus de 1000 objets dans un IN
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
		}
		finally {
			parent.setEnabledForThread(enabled);
		}

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}
}
