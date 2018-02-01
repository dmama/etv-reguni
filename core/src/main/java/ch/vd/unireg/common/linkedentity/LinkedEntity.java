package ch.vd.unireg.common.linkedentity;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Interface qui spécifie que l'entité hibernate courante pointe vers une ou plusieurs autres entités. Le type de relation (parent/enfant) n'est pas précisé.
 */
public interface LinkedEntity {

	/**
	 * @param context        le context dans lequel on demande la liste des entités liées.
	 * @param includeAnnuled <b>vrai</b> s'il faut tenir compte des liens annulés (utile dans le cas d'une annulation de rapport-entre-tiers, par exemple); ou <b>faux</b> s'il ne faut pas en tenir compte.
	 * @return la liste des entités liées; ou <b>null</b> s'il n'y en a pas. Cette liste pour contenir les entités elles-mêmes (de type {@link ch.vd.unireg.common.HibernateEntity}) ou leurs clés ({@link ch.vd.unireg.common.EntityKey}).
	 */
	List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled);
}
