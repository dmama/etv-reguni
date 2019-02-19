package ch.vd.unireg.validation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.shared.validation.ValidationException;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.EntityKey;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.common.ThreadSwitch;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;

public class ValidationInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean, Switchable {

	private final ThreadSwitch enabled = new ThreadSwitch(true);

	private ModificationInterceptor parent;
	private HibernateTemplate hibernateTemplate;
	private ValidationService validationService;

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
		if (!isEnabled()) {
			return false;
		}

		// old-TO-DO (msi) mémoriser l'entité et déplacer la validation dans le preTransactionCommit.
		// Non, en fait il n'est pas possible de déplacer la validation juste avant le commit de la transaction.
		// Le problème est que les durées de vie de la transaction Spring et de la session Hibernate ne sont pas absolument pareilles. Dans certains cas, le preTransactionCommit sera appelé alors que
		// la session Spring est encore valide (pour les transactions ouvertes par les @EmbeddedInTransaction, car le session sera fermée par un listener sur le même preTransactionCommit), mais dans d'autres
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
	public void suspendTransaction() {
		// rien à faire ici
	}

	@Override
	public void resumeTransaction() {
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
		final Set<Object> visited = new HashSet<>();
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
			final List<?> linked = entity.getLinkedEntities(new LinkedEntityContext(LinkedEntityPhase.VALIDATION, hibernateTemplate), isAnnulation);
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

	@Override
	public void setEnabled(boolean value) {
		this.enabled.setEnabled(value);
	}

	@Override
	public boolean isEnabled() {
		return enabled.isEnabled();
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

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}
}
