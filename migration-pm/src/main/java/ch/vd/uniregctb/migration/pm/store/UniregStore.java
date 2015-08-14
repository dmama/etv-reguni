package ch.vd.uniregctb.migration.pm.store;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.migration.pm.mapping.MigrationPmMapping;

/**
 * Interface d'abstraction de la couche de persistence Unireg (permet d'éventuellement fournir une autre implémentation de cette couche
 * pendant les tests)
 * <br/>
 * <b>Notes</b>&nbsp;:
 * <ol>
 *     <li>la quasi-totalité des méthodes utilise un type générique contraint (<code>E extends HibernateEntity</code>) afin
 *     d'éviter les boulettes en appelant ces méthodes avec une entité de RegPM (ces entités n'héritent pas de {@link HibernateEntity})</li>
 *     <li>comme l'entité spécifique {@link MigrationPmMapping} n'hérite pas non plus de {@link HibernateEntity}, bien qu'étant présente
 *     dans l'univers Unireg, des méthodes spécifiques ont été ajoutées explicitement</li>
 * </ol>
 */
public interface UniregStore {

	/**
	 * @param clazz la classe de l'entité à récupérer depuis la base de données Unireg
	 * @param id identifiant de l'entité à récupérer
	 * @param <E> type de l'entité récupérée
	 * @return l'entité avec l'identifiant donné
	 */
	@Nullable
	<E extends HibernateEntity> E getEntityFromDb(Class<E> clazz, long id);

	/**
	 * Méthode qui permet d'aller chercher des entités dans la base de données Unireg sans forcément connaître leur identifiant
	 * @param clazz classe des entités visées
	 * @param criteria critères (attribut / valeur)
	 * @param <E> type des entitées visées
	 * @return la liste des entités trouvées
	 */
	<E extends HibernateEntity> List<E> getEntitiesFromDb(Class<E> clazz, Map<String, ?> criteria);

	/**
	 * Méthode qui permet d'aller chercher l'intégralité des entités d'une classe donnée (y compris les sous-classes)
	 * @param clazz classe des entités recherchées
	 * @param <E> type des entités visées
	 * @return un itérateur sur les entités trouvées
	 */
	<E extends HibernateEntity> Iterator<E> iterateOnAllEntities(Class<E> clazz);

	/**
	 * @param entity l'entité à sauvegarder en base de données Unireg
	 * @param <E> type de l'entité
	 * @return l'entité après sauvegarde
	 */
	<E extends HibernateEntity> E saveEntityToDb(E entity);

	/**
	 * @param entity l'entité à supprimer de la base (= à ne pas sauvegarder, en fait...)
	 */
	void removeEntityFromDb(HibernateEntity entity);

	/**
	 * Méthode qui permet de récupérer un à un les éléments de mapping persistés
	 * @return un itérateur sur les éléments de mapping persistés
	 */
	Iterator<MigrationPmMapping> iterateOnAllMappingEntities();

	/**
	 * @param entity l'entité de mapping à sauvegarder
	 * @return l'entité après sauvegarde (l'ID est renseigné, par exemple...)
	 */
	MigrationPmMapping saveMappingEntityToDb(MigrationPmMapping entity);
}
