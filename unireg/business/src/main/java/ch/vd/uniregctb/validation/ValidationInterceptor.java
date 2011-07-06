package ch.vd.uniregctb.validation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.LinkedEntity;

public class ValidationInterceptor implements ModificationSubInterceptor, InitializingBean {

	private static class Behavior {
		public boolean enabled = true;
	}

	private ModificationInterceptor parent;
	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();
	private HibernateTemplate hibernateTemplate;
	private ValidationService validationService;

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws
			CallbackException {
		if (!isEnabled()) {
			return false;
		}

		// old-TO-DO (msi) mémoriser l'entité et déplacer la validation dans le preTransactionCommit.
		// Non, en fait il n'est pas possible de déplacer la validation juste avant le commit de la transaction.
		// Le problème est que les durées de vie de la transaction Spring et de la session Hibernate ne sont pas absolument pareilles. Dans certains cas, le preTransactionCommit sera appelé alors que
		// la session Spring est encore valide (pour les transactions ouvertes par les @Transactional, car le session sera fermée par un listener sur le même preTransactionCommit), mais dans d'autres
		// cas (par exemple les batches qui ouvrent une transaction puis une session nestée à travers le BatchTransactionTemplate) la session est déjà invalide lors de la fermeture de la transaction
		// et les collections Hibernate non-initialisées font sauter une exception lors de la validation.
		validate(entity, isAnnulation);
		return false;
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
		// rien à faire ici
	}

	@Override
	public void postTransactionRollback() {
		// rien à faire ici
	}

	/**
	 * Valide l'objet spécifié et tous les objets liés qui peuvent l'être.
	 *
	 * @param object       l'objet à valider
	 * @param isAnnulation vrai si l'objet à valider vient d'être annulé.
	 * @throws ValidationException en cas d'erreur de validation
	 */
	private void validate(Object object, boolean isAnnulation) throws ValidationException {
		final Set<Object> visited = new HashSet<Object>();
		validate(object, isAnnulation, visited);
	}

	private void validate(Object object, boolean isAnnulation, Set<Object> visited) {

		if (visited.contains(object)) {
			return;
		}

		final ValidationResults results = validationService.validate(object);
		if (results.hasErrors()) {
			throw new ValidationException(object, results.getErrors(), results.getWarnings());
		}

		visited.add(object);

		// si l'objet pointe vers d'autres objets validables, on les valide aussi
		if (object instanceof LinkedEntity) {
			final LinkedEntity entity =(LinkedEntity) object;
			final List<?> linked = entity.getLinkedEntities(isAnnulation);
			if (linked != null) {
				for (Object o : linked) {
					if (o instanceof EntityKey) {
						final EntityKey k = (EntityKey) o;
						o = hibernateTemplate.get(k.getClazz(), (Serializable) k.getId());
					}
					validate(o, false, visited);
				}
			}
		}
	}

	private Behavior getByThreadBehavior() {
		Behavior behavior = this.byThreadBehavior.get();
		if (behavior == null) {
			behavior = new Behavior();
			this.byThreadBehavior.set(behavior);
		}
		return behavior;
	}

	public void setEnabled(boolean value) {
		getByThreadBehavior().enabled = value;
	}

	public boolean isEnabled() {
		return getByThreadBehavior().enabled;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
