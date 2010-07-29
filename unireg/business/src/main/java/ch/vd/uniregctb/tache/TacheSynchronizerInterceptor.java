package ch.vd.uniregctb.tache;

import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.validation.SubValidateable;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * [UNIREG-2305] Cet interceptor recalcul automatiquement les tâches d'envoi et d'annulation de DIs sur les contribuables modifiées après le commit de chaque transaction.
 */
public class TacheSynchronizerInterceptor implements ModificationSubInterceptor, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TacheSynchronizerInterceptor.class);

	private ModificationInterceptor parent;
	private HibernateTemplate hibernateTemplate;
	private TacheService tacheService;
	private TransactionManager transactionManager;

	private final ThreadLocal<HashSet<Long>> modifiedCtbIds = new ThreadLocal<HashSet<Long>>();
	private final ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();

	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {

		if (isDisabled()) {
			return false;
		}

		if (entity instanceof Contribuable) {
			final Contribuable ctb = (Contribuable) entity;
			addModifiedContribuable(ctb);
		}
		else if (entity instanceof SubValidateable) {
			final SubValidateable sub = (SubValidateable) entity;
			final Validateable master = sub.getMaster();
			if (master instanceof Contribuable) {
				final Contribuable ctb = (Contribuable) master;
				addModifiedContribuable(ctb);
			}
		}

		return false;
	}

	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	public void preTransactionCommit() {
		// rien à faire ici
	}

	public void postTransactionCommit() {

		final HashSet<Long> set = getModifiedCtbIds();
		if (set.isEmpty()) {
			return; // rien à faire
		}

		try {
			parent.setEnabledForThread(false); // on désactive l'intercepteur pour éviter de s'intercepter soi-même

			final TransactionTemplate template = new TransactionTemplate((PlatformTransactionManager) transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					synchronizeTaches(set);
					return null;
				}
			});
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			parent.setEnabledForThread(true);
			set.clear();
		}
	}

	private void synchronizeTaches(final HashSet<Long> ctbIds) {
		hibernateTemplate.executeWithNewSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				for (Long id : ctbIds) {
					final Contribuable ctb = (Contribuable) session.get(Contribuable.class, id);
					if (ctb != null) {
						tacheService.synchronizeTachesDIs(ctb);
					}
				}
				return null;
			}
		});
	}

	public void postTransactionRollback() {
		final HashSet<Long> set = getModifiedCtbIds();
		set.clear();
	}

	public void setOnTheFlySynchronization(boolean value) {
		disabled.set(!value);
	}

	private boolean isDisabled() {
		return disabled.get() != null && disabled.get();
	}

	/**
	 * Ajoute le contribuable spécifié dans les liste des contribuables qui seront indéxés après le flush.
	 *
	 * @param ctb le contribuable en question.
	 */
	private void addModifiedContribuable(Contribuable ctb) {
		if (ctb != null) {
			getModifiedCtbIds().add(ctb.getNumero());
		}
	}

	private HashSet<Long> getModifiedCtbIds() {
		HashSet<Long> ent = modifiedCtbIds.get();
		if (ent == null) {
			ent = new HashSet<Long>();
			modifiedCtbIds.set(ent);
		}
		return ent;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
