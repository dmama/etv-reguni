package ch.vd.uniregctb.validation;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.LinkedEntity;

public class ValidationInterceptor implements ModificationSubInterceptor, InitializingBean, Switchable {

	private final ThreadSwitch enabled = new ThreadSwitch(true);

	private final ThreadLocal<Set<Object>> toValidate = new ThreadLocal<Set<Object>>() {
		@Override
		protected Set<Object> initialValue() {
			return new LinkedHashSet<>();
		}
	};

	private ModificationInterceptor parent;
	private HibernateTemplate hibernateTemplate;
	private ValidationService validationService;

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws  CallbackException {
		if (!isEnabled()) {
			return false;
		}

		// on mémorise les entités à valider (see #postFlush())
		final Set<Object> toValidate = new LinkedHashSet<>();
		fillInstancesToValidate(entity, isAnnulation, toValidate);
		this.toValidate.get().addAll(toValidate);

		return false;
	}

	private void fillInstancesToValidate(Object source, boolean isAnnulation, Set<Object> toValidate) {
		// déjà là -> on ne va pas plus loin (pas besoin)
		if (toValidate.contains(source)) {
			return;
		}

		// on le rajoute, à la fois pour remplir l'ensemble et pour bloquer les récursions infinies
		toValidate.add(source);

		// si l'objet pointe vers d'autres objets validables, on les validera aussi
		if (source instanceof LinkedEntity) {
			final LinkedEntity entity = (LinkedEntity) source;
			final List<?> linked = entity.getLinkedEntities(isAnnulation);
			if (linked != null) {
				for (Object o : linked) {
					if (o instanceof EntityKey) {
						final EntityKey k = (EntityKey) o;
						o = hibernateTemplate.get(k.getClazz(), (Serializable) k.getId());
					}
					fillInstancesToValidate(o, false, toValidate);
				}
			}
		}
	}

	@Override
	public void postFlush() throws CallbackException {
		try {
			if (!isEnabled()) {
				return;
			}

			// boucle sur tous les objets recueillis, et validation de chacun d'eux
			for (Object object : this.toValidate.get()) {
				final ValidationResults results = validationService.validate(object);
				if (results.hasErrors()) {
					throw new ValidationException(object, results.getErrors(), results.getWarnings());
				}
			}
		}
		finally {
			// on enlève tout pour la prochaine fois
			this.toValidate.get().clear();
		}
	}

	@Override
	public void preTransactionCommit() {
		// on est censé être passé par un flush...
		final Set<?> toValidate = this.toValidate.get();
		try {
			if (!toValidate.isEmpty()) {
				throw new IllegalStateException("Il y a encore " + toValidate.size() + " entité(s) à valider au moment du pré-commit (pas de flush ??)...");
			}
		}
		finally {
			// on enlève tout pour la prochaine fois
			toValidate.clear();
		}
	}

	@Override
	public void postTransactionCommit() {
		// on est censé être passé par un flush...
		final Set<?> toValidate = this.toValidate.get();
		try {
			if (!toValidate.isEmpty()) {
				throw new IllegalStateException("Il y a encore " + toValidate.size() + " entité(s) à valider au moment du post-commit (pas de flush ??)...");
			}
		}
		finally {
			// on enlève tout pour la prochaine fois
			toValidate.clear();
		}
	}

	@Override
	public void postTransactionRollback() {
		// on enlève tout pour la prochaine fois
		this.toValidate.get().clear();
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
}
