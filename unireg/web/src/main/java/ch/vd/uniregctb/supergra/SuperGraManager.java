package ch.vd.uniregctb.supergra;

import java.util.List;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Manager du mode SuperGra de Unireg. Il est responsable des opérations métier permettant de charger, créer et sauver les entités hibernate manipulées.
 */
public interface SuperGraManager {

	void fillView(EntityKey key, EntityView view, List<Delta> deltas);

	void fillView(EntityKey key, String collName, CollectionView view, List<Delta> deltas);

	/**
	 * Alloue et retourne le prochain id valable pour une nouvelle entité de la classe spécifiée.
	 *
	 * @param clazz une classe qui représente une entité hibernate
	 * @return le nouvelle id alloué
	 */
	Long nextId(Class<? extends HibernateEntity> clazz);
}
