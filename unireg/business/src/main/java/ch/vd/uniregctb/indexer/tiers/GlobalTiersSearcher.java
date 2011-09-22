package ch.vd.uniregctb.indexer.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersFilter;

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
	 * Recherche et retourne les tiers qui correspondent le mieux aux critères spécifiés
	 *
	 * @param criteria les critères de recherche
	 * @param max      le nombre maximum de résultats à retourner
	 * @return une liste de données de tiers
	 * @throws IndexerException en cas d'erreur levée dans l'indexeur
	 */
	public TopList<TiersIndexedData> searchTop(TiersCriteria criteria, int max) throws IndexerException;

	/**
	 * Recherche et retourne les tiers qui correspondent le mieux aux mot-clés spécifiés.
	 *
	 * @param keywords les mots-clés de recherche
	 * @param filter   un filtre (optionnel) pour filtrer certaines catégories de tiers
	 * @param max      le nombre maximum de résultats à retourner  @return une liste de données de tiers
	 * @throws IndexerException en cas d'erreur levée dans l'indexeur
	 * @return une liste de données de tiers
	 */
	public TopList<TiersIndexedData> searchTop(String keywords, TiersFilter filter, int max) throws IndexerException;

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

	/**
	 * La méthode docCount renvoie le nombre de documents dans l'index, qui comprend aussi les documents effacés et non-purgés
	 *
	 * @return le nombre de documents dans l'index, y compris les effacés
	 */
	int getApproxDocCount();
	
	/**
	 * La méthode docCount renvoie le nombre de documents dans l'index, qui comprend aussi les documents effacés et non-purgés
	 * optimize() purge l'index donc si on fait un optimize() avant un docCount() on a le nombre de documents exact
	 *
	 * @return le nombre de documents dans l'index
	 */
	int getExactDocCount();
}
