package ch.vd.uniregctb.tiers;

import java.util.List;

/**
 * Interface qui spécifie que l'entité hibernate courante pointe vers une ou plusieurs autres entités. Le type de relation (parent/enfant) n'est pas précisé.
 */
public interface LinkedEntity {

	/**
	 * @return la liste des entités liées. Cette liste pour contenir les entités elles-mêmes (de type {@link ch.vd.uniregctb.common.HibernateEntity}) ou leurs clés ({@link
	 *         ch.vd.uniregctb.common.EntityKey}).
	 */
	List<?> getLinkedEntities();
}
