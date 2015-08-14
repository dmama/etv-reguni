package ch.vd.uniregctb.migration.pm.store;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.migration.pm.mapping.MigrationPmMapping;

/**
 * Implémentation concrète de la couche de persistence Unireg pendant la migration (= celle qui va effectivement en base)
 */
public class UniregStoreImpl implements UniregStore {

	private final SessionFactory uniregSessionFactory;

	public UniregStoreImpl(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	/**
	 * @param clazz la classe de l'entité à récupérer depuis la base de données Unireg
	 * @param id identifiant de l'entité à récupérer
	 * @param <E> type de l'entité récupérée
	 * @return l'entité avec l'identifiant donné
	 */
	@Nullable
	@Override
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
	@Override
	public final <E extends HibernateEntity> List<E> getEntitiesFromDb(Class<E> clazz, Map<String, ?> criteria) {
		final Criteria c = uniregSessionFactory.getCurrentSession().createCriteria(clazz);
		if (criteria != null) {
			criteria.forEach((key, value) -> c.add(Restrictions.eq(key, value)));
		}
		c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		//noinspection unchecked
		return c.list();
	}

	/**
	 * Méthode qui permet d'aller chercher l'intégralité des entités d'une classe donnée (y compris les sous-classes)
	 * @param clazz classe des entités recherchées
	 * @param <E> type des entités visées
	 * @return un itérateur sur les entités trouvées
	 */
	@Override
	public final <E extends HibernateEntity> Iterator<E> iterateOnAllEntities(Class<E> clazz) {
		return iterateOnAllEntitiesOfClass(clazz, uniregSessionFactory.getCurrentSession());
	}

	/**
	 * Méthode qui permet de récupérer un à un les éléments de mapping persistés
	 * @return un itérateur sur les éléments de mapping persistés
	 */
	@Override
	public final Iterator<MigrationPmMapping> iterateOnAllMappingEntities() {
		return iterateOnAllEntitiesOfClass(MigrationPmMapping.class, uniregSessionFactory.getCurrentSession());
	}

	private static <E> Iterator<E> iterateOnAllEntitiesOfClass(Class<E> clazz, Session currentSession) {
		final Query query = currentSession.createQuery("from " + clazz.getName());
		//noinspection unchecked
		return query.iterate();
	}

	/**
	 * @param entity l'entité à sauvegarder en base de données Unireg
	 * @param <E> type de l'entité
	 * @return l'entité après sauvegarde
	 */
	@Override
	public final <E extends HibernateEntity> E saveEntityToDb(E entity) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().merge(entity);
	}

	/**
	 * @param entity l'entité à supprimer de la base (= à ne pas sauvegarder, en fait...)
	 */
	@Override
	public void removeEntityFromDb(HibernateEntity entity) {
		uniregSessionFactory.getCurrentSession().delete(entity);
	}

	/**
	 * @param entity l'entité de mapping à sauvegarder
	 * @return l'entité après sauvegarde (l'ID est renseigné, par exemple...)
	 */
	@Override
	public final MigrationPmMapping saveMappingEntityToDb(MigrationPmMapping entity) {
		return (MigrationPmMapping) uniregSessionFactory.getCurrentSession().merge(entity);
	}
}
