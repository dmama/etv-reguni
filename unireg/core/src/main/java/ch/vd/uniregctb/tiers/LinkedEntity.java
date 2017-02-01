package ch.vd.uniregctb.tiers;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Interface qui spécifie que l'entité hibernate courante pointe vers une ou plusieurs autres entités. Le type de relation (parent/enfant) n'est pas précisé.
 */
public interface LinkedEntity {

	enum Context {
		/**
		 * On demande les entités liées dans le context de la validation des données avant sauvegarde dans la base. Les entités retournées seront elles-mêmes valides.
		 */
		VALIDATION,
		/**
		 * On demande les entités liées dans le context de l'indexation des données après sauvegarde dans la base. Les entités retournées seront elles-mêmes indexées.
		 */
		INDEXATION,
		/**
		 * On demande les entités liées dans le context du recalcul des parentés entre personnes physiques.
		 */
		PARENTES,
		/**
		 * On demande les entités liées dans le context du recalcul des tâches sur les contribuables.
		 */
		TACHES,
		/**
		 * On demande les entités liées dans le context de l'envoi d'événements de changement internes (pour de l'invalidation des divers caches applicatifs, entres autres). Les entités retournées provoqueront l'émission d'autant d'événements
		 * internes.
		 */
		DATA_EVENT
	}

	/**
	 * @param context        le context pour lequel on demande la liste des entités liées.
	 * @param includeAnnuled <b>vrai</b> s'il faut tenir compte des liens annulés (utile dans le cas d'une annulation de rapport-entre-tiers, par exemple); ou <b>faux</b> s'il ne faut pas en tenir compte.
	 * @return la liste des entités liées; ou <b>null</b> s'il n'y en a pas. Cette liste pour contenir les entités elles-mêmes (de type {@link ch.vd.uniregctb.common.HibernateEntity}) ou leurs clés ({@link ch.vd.uniregctb.common.EntityKey}).
	 */
	List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled);
}
