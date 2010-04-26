package ch.vd.uniregctb.evenement.engine;

import ch.vd.uniregctb.common.StatusManager;


/**
 * Service chargé de lire les nouveaux événements unitaires insérés en base de données et de tenter de les regrouper.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 */
public interface EvenementCivilRegrouper {

	/**
	 * lit les événements unitaires non-traités insérés en base de données et tente de les regrouper.
	 *
	 * @param status un status manager (optionel, peut être nul)
	 */
	void regroupeTousEvenementsNonTraites(StatusManager status);

	/**
	 * Regroupe l'evt unitaire passé en paramètre
	 *
	 * @param id       l'id de l'événement unitaire à regrouper
	 * @param errorMsg un message ajouté à l'exception en cas d'erreur
	 * @return l'ID de l'evt regroupé, -1 en cas d'erreur
	 */
	long regroupeUnEvenementById(long id, StringBuffer errorMsg);
}
