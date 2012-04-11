package ch.vd.uniregctb.tiers;

import java.util.List;

/**
 * Interface qui spécifie que l'entité hibernate courante pointe vers une ou plusieurs autres entités. Le type de relation (parent/enfant) n'est pas précisé.
 */
public interface LinkedEntity {

	/**
	 * @param includeAnnuled <b>vrai</b> s'il faut tenir compte des liens annulés (utile dans le cas d'une annulation de rapport-entre-tiers, par exemple); ou <b>faux</b> s'il ne faut pas en tenir
	 *                       compte.
	 * @return la liste des entités liées; ou <b>null</b> s'il n'y en a pas. Cette liste pour contenir les entités elles-mêmes (de type {@link ch.vd.uniregctb.common.HibernateEntity}) ou leurs clés
	 *         ({@link ch.vd.uniregctb.common.EntityKey}).
	 */
	List<?> getLinkedEntities(boolean includeAnnuled);
}
