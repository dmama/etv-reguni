package ch.vd.uniregctb.migration.pm.store;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;

public class UniregStore {
	protected SessionFactory uniregSessionFactory;

	public UniregStore(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	/**
	 * @param clazz la classe de l'entité à récupérer depuis la base de données Unireg
	 * @param id identifiant de l'entité à récupérer
	 * @param <E> type de l'entité récupérée
	 * @return l'entité avec l'identifiant donné
	 */
	@Nullable
	public final <E extends HibernateEntity> E getEntityFromDb(Class<E> clazz, long id) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().get(clazz, id);
	}

	/**
	 * Méthode qui permet d'aller chercher des entités dans la base de données Unireg sans forcément connaître leur identifiant
	 * @param clazz classe des entités visées
	 * @param criteria critères (attribut / valeur)
	 * @param <E> type des entitées visées
	 * @return la liste des entités trouvées
	 */
	public final <E extends HibernateEntity> List<E> getEntitiesFromDb(Class<E> clazz, Map<String, ?> criteria) {
		final Criteria c = uniregSessionFactory.getCurrentSession().createCriteria(clazz);
		if (criteria != null) {
			criteria.forEach((key, value) -> c.add(Restrictions.eq(key, value)));
		}
		//noinspection unchecked
		return c.list();
	}

	/**
	 * @param entity l'entité à sauvegarder en base de données Unireg
	 * @param <E> type de l'entité
	 * @return l'entité après sauvegarde
	 */
	public final <E extends HibernateEntity> E saveEntityToDb(E entity) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().merge(entity);
	}

}
