package ch.vd.uniregctb.migration.pm.utils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.shared.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.validation.ValidationService;

public class ValidationInterceptor extends AbstractLinkedInterceptor {

	private static final String ANNULATION_DATE = "annulationDate";

	private ValidationService validationService;
	private SessionFactory sessionFactory;

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
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
			if (ANNULATION_DATE.equals(name)) {
				index = i;
				break;
			}
		}
		// si la date d'annulation était nulle et qu'elle ne l'est plus, alors on affaire à une annulation
		return index >= 0 && previousState[index] == null && currentState[index] != null;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		// old-TO-DO (msi) mémoriser l'entité et déplacer la validation dans le preTransactionCommit.
		// Non, en fait il n'est pas possible de déplacer la validation juste avant le commit de la transaction.
		// Le problème est que les durées de vie de la transaction Spring et de la session Hibernate ne sont pas absolument pareilles. Dans certains cas, le preTransactionCommit sera appelé alors que
		// la session Spring est encore valide (pour les transactions ouvertes par les @Transactional, car le session sera fermée par un listener sur le même preTransactionCommit), mais dans d'autres
		// cas (par exemple les batches qui ouvrent une transaction puis une session nestée à travers le BatchTransactionTemplate) la session est déjà invalide lors de la fermeture de la transaction
		// et les collections Hibernate non-initialisées font sauter une exception lors de la validation.
		validate(entity, false);
		return false;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		final boolean isAnnulation = detectAnnulation(currentState, previousState, propertyNames);

		// old-TO-DO (msi) mémoriser l'entité et déplacer la validation dans le preTransactionCommit.
		// Non, en fait il n'est pas possible de déplacer la validation juste avant le commit de la transaction.
		// Le problème est que les durées de vie de la transaction Spring et de la session Hibernate ne sont pas absolument pareilles. Dans certains cas, le preTransactionCommit sera appelé alors que
		// la session Spring est encore valide (pour les transactions ouvertes par les @Transactional, car le session sera fermée par un listener sur le même preTransactionCommit), mais dans d'autres
		// cas (par exemple les batches qui ouvrent une transaction puis une session nestée à travers le BatchTransactionTemplate) la session est déjà invalide lors de la fermeture de la transaction
		// et les collections Hibernate non-initialisées font sauter une exception lors de la validation.
		validate(entity, isAnnulation);
		return false;
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
			final LinkedEntity entity = (LinkedEntity) object;
			final List<?> linked = entity.getLinkedEntities(isAnnulation);
			if (linked != null) {
				for (Object o : linked) {
					if (o instanceof EntityKey) {
						final EntityKey k = (EntityKey) o;
						o = sessionFactory.getCurrentSession().get(k.getClazz(), (Serializable) k.getId());
					}
					validate(o, false, visited);
				}
			}
		}
	}
}
