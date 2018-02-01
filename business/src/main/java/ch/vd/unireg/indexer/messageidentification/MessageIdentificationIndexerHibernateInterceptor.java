package ch.vd.unireg.indexer.messageidentification;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.StackedThreadLocal;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.common.ThreadSwitch;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;

public class MessageIdentificationIndexerHibernateInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean, Switchable {

	private ModificationInterceptor parent;
	private GlobalMessageIdentificationIndexer indexer;
	private PlatformTransactionManager transactionManager;

	private final ThreadSwitch enabled = new ThreadSwitch(true);

	private final StackedThreadLocal<Set<Long>> modifiedEntities = new StackedThreadLocal<>(HashSet::new);

	private Set<Long> getModifiedEntities() {
		return modifiedEntities.get();
	}

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setIndexer(GlobalMessageIdentificationIndexer indexer) {
		this.indexer = indexer;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}

	/**
	 * Méthode appelée quand une entité Hibernate est modifiée
	 * @param entity        l'entité qui a changé.
	 * @param id            l'id de l'entité.
	 * @param currentState  l'état après changement de l'entité.
	 * @param previousState l'état avant changement de l'entité; ou <b>null</b> s'il s'agit d'une entité nouvellement créée.
	 * @param propertyNames les noms de propriétés associées aux valeurs des paramètres <i>currentState</i> et <i>previousState</i>.
	 * @param types         les types de propriétés associées aux valeurs des paramètres <i>currentState</i> et <i>previousState</i>.
	 * @param isAnnulation  <b>vrai</b> si l'entité a été nouvellement annulée; <b>faux</b> autrement.
	 * @return <code>true</code> si la méthode a modifié l'entité, <code>false</code> sinon
	 * @throws CallbackException en cas de souci...
	 */
	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
		if (!isEnabled()) {
			return false;
		}

		if (entity instanceof IdentificationContribuable) {
			final Long identifier = (Long) id;
			if (identifier != null) {
				getModifiedEntities().add(identifier);
			}
		}
		return false;
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire
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
		// rien à faire
	}

	@Override
	public void postTransactionCommit() {
		try {
			indexModifiedEntities();
		}
		finally {
			getModifiedEntities().clear();
		}
	}

	@Override
	public void postTransactionRollback() {
		getModifiedEntities().clear();
	}

	/**
	 * C'est ici que tout le boulot se fait... toute nouvelle demande d'identification de contribuable, ou toute modification sur une telle demande,
	 * doit forcer la ré-indexation de l'entité
	 */
	private void indexModifiedEntities() {
		if (isEnabled()) {
			final Set<Long> ids = getModifiedEntities();
			if (!ids.isEmpty()) {
				final TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.setReadOnly(true);
				template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						for (Long id : ids) {
							indexer.reindex(id);
						}
					}
				});
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return this.enabled.isEnabled();
	}
}
