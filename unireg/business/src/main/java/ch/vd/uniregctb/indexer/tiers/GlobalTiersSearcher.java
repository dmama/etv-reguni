package ch.vd.uniregctb.indexer.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.TiersCriteria;

/**
 * Classe principale de recherche de tiers suivant certains criteres
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 *
 */
public interface GlobalTiersSearcher {

	/**
	 * Methode principale de recherche des tiers
	 *
	 * @param criteria
	 * @return la liste des tiers repondant aux criteres de recherche
	 * @throws IndexerException
	 */
	public List<TiersIndexedData> search(TiersCriteria criteria) throws IndexerException;

	/**
	 * Vérifie si un tiers est indexé ou non.
	 *
	 * @param numero
	 *            le numéro du tiers à tester.
	 * @return <b>vrai</b> si le tiers spécifié est indexé.
	 */
	public boolean exists(Long numero) throws IndexerException;

	/**
	 * Retourne les informations indexées pour le tiers spécifié.
	 *
	 * @param numero
	 *            le numéro du tiers
	 * @return les informations indexées, ou <b>null</b> si le tiers n'existe pas ou n'est pas indexé
	 */
	public TiersIndexedData get(Long numero) throws IndexerException;

	/**
	 * @return la liste de tous les IDs des tiers indexés
	 */
	public Set<Long> getAllIds();

	/**
	 * Vérifie la cohérence des données de l'indexeur. L'indexeur n'est pas modifié.
	 *
	 * @param existingIds
	 *            les ids des tiers existant réellement dans la base de données.
	 * @param statusManager
	 *            le status manager
	 * @param callback
	 *            le callback permettant d'être notifié à chaque erreur ou warning trouvé
	 *
	 * @return le résultat de la validation
	 */
	public void checkCoherenceIndex(Set<Long> existingIds, StatusManager statusManager, CheckCallback callback);

	/**
	 * Interface de callback de la méthode {@link GlobalTiersSearcher#checkCoherenceIndex(Set, StatusManager, CheckCallback)}.
	 */
	public static interface CheckCallback {
		/**
		 * Une erreur a été trouvée sur le tiers spécifié.
		 *
		 * @param id
		 *            l'id du tiers
		 * @param message
		 *            un message décrivant le problème
		 */
		void onError(long id, String message);

		/**
		 * Un warning a été trouvé sur le tiers spécifié.
		 *
		 * @param id
		 *            l'id du tiers
		 * @param message
		 *            un message décrivant le problème
		 */
		void onWarning(long id, String message);
	}

	public GlobalIndexInterface getGlobalIndex();

}
