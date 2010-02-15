package ch.vd.uniregctb.evenement.engine;


/**
 * Service chargé de lire les nouveaux événements unitaires insérés en base de données
 * et de tenter de les regrouper.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public interface EvenementCivilRegrouper {

	/**
	 * lit les événements unitaires non-traités insérés en base de données
  	 * et tente de les regrouper.
	 */
	void regroupeTousEvenementsNonTraites();

	/**
	 * Regroupe l'evt unitaire passé en paramètre
	 *
	 * @param evenement
	 * @return
	 */
	long regroupeUnEvenementById(long id, StringBuffer errorMsg);

}
