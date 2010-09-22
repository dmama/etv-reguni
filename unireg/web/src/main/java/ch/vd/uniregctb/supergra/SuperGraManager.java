package ch.vd.uniregctb.supergra;

import java.util.List;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.delta.Delta;
import ch.vd.uniregctb.supergra.view.CollectionView;
import ch.vd.uniregctb.supergra.view.EntityView;

/**
 * Manager du mode SuperGra de Unireg. Il est responsable des opérations métier permettant de charger, créer et sauver les entités hibernate manipulées.
 */
public interface SuperGraManager {

	void fillView(EntityKey key, EntityView view, SuperGraSession session);

	void fillView(EntityKey key, String collName, CollectionView view, SuperGraSession session);

	/**
	 * Alloue et retourne le prochain id valable pour une nouvelle entité de la classe spécifiée.
	 *
	 * @param clazz une classe qui représente une entité hibernate
	 * @return le nouvelle id alloué
	 */
	Long nextId(Class<? extends HibernateEntity> clazz);

	/**
	 * Applique les modifications aux entités de la base de données et sauvegarde le tout.
	 *
	 * @param deltas les modifications à appliquer et sauver.
	 */
	void commitDeltas(List<Delta> deltas);
}
