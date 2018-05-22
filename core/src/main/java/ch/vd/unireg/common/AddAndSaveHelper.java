package ch.vd.unireg.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Helper pour les problèmatiques de addAndSave rencontrées dans les tiers, les déclarations...
 */
public abstract class AddAndSaveHelper {

	/**
	 * Interface des accès aux entités depuis un container
	 * @param <C> la classe du container
	 * @param <E> la classe des entités contenues
	 */
	public interface EntityAccessor<C, E> {
		/**
		 * @param container un container
		 * @return la collection des entités du container
		 */
		Collection<? extends HibernateEntity> getEntities(C container);

		/**
		 * Associe une nouvelle entité au container
		 * @param container le container
		 * @param entity l'entité à lui associer
		 */
		void addEntity(C container, E entity);

		/**
		 * Blindage, vérification que les deux entités sont bien les mêmes
		 * @param entity1 une entité
		 * @param entity2 une autre entité
		 */
		void assertEquals(E entity1, E entity2);
	}

	/**
	 * Ajoute au container une nouvelle entité contenue, et renvoie cette nouvelle entité après sauvegarde
	 * @param container le container
	 * @param entity l'entité à ajouter
	 * @param persister fonction qui lance la persistance du container, et renvoie la valeur persistée
	 * @param accessor les méthodes d'accès à la collection d'entités et à l'ajout d'une nouvelle entité dans le container
	 * @param <C> le type du container
	 * @param <E> le type de l'entité
	 * @return l'entité après sauvegarde (= avec un identifiant renseigné)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends HibernateEntity, E extends HibernateEntity> E addAndSave(C container, E entity, UnaryOperator<C> persister, EntityAccessor<C, E> accessor) {

		if (entity.getKey() == null) {
			// pas encore persistée

			// on mémorise les clés des entités existantes
			final Set<Object> keys;
			final Collection<? extends HibernateEntity> entities = accessor.getEntities(container);
			if (entities == null || entities.isEmpty()) {
				keys = Collections.emptySet();
			}
			else {
				keys = new HashSet<>(entities.size());
				for (HibernateEntity d : entities) {
					final Object key = d.getKey();
					if (key == null) {
						throw new IllegalArgumentException("Les entités existantes doivent être déjà persistées.");
					}
					keys.add(key);
				}
			}

			// on ajoute la nouvelle entité et on sauve le tout
			accessor.addEntity(container, entity);
			container = persister.apply(container);

			// rebelotte pour trouver la nouvelle entité
			HibernateEntity newEntity = null;
			for (HibernateEntity d : accessor.getEntities(container)) {
				if (!keys.contains(d.getKey())) {
					newEntity = d;
					break;
				}
			}

			if (newEntity == null) {
				throw new IllegalArgumentException();
			}
			accessor.assertEquals(entity, (E) newEntity);
			entity = (E) newEntity;
		}
		else {
			// déjà persistée, on a juste à remplir la collection
			accessor.addEntity(container, entity);
		}

		if (entity.getKey() == null) {
			throw new IllegalArgumentException();
		}
		return entity;
	}
}
