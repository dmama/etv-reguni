package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public interface RapportEntreTiersDAO extends GenericDAO<RapportEntreTiers, Long> {

	/**
	 * @param noTiersTuteur
	 * @param noTiersPupille
	 * @return
	 */
	List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille);

	/**
	 * @param noTiersTuteur
	 * @param noTiersPupille
	 * @param doNotAutoFlush
	 * @return
	 */
	List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille, boolean doNotAutoFlush);

	/**
	 * Retourne les rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur  un numéro de débiteur
	 * @param paramPagination des éventuels paramètres de pagination
	 * @param activesOnly     <b>vrai</b> s'il ne faut retourner que les rapports actifs à l'heure actuelle; <b>faux</b> pour retourner tous les rapports existants.
	 * @return les rapports trouvés.
	 */
	List<RapportPrestationImposable> getRapportsPrestationImposable(Long numeroDebiteur, ParamPagination paramPagination, boolean activesOnly);


	/**
	 * Retourne les rapports prestation imposable existant entre un débiteur et un sourcier
	 *
	 *
	 * @param numeroDebiteur  un numéro de débiteur
	 * @param numeroSourcier un numéro de sourcier
	 * @param activesOnly     <b>vrai</b> s'il ne faut retourner que les rapports actifs à l'heure actuelle; <b>faux</b> pour retourner tous les rapports existants.
	 * @param doNotAutoFlush
	 * @return les rapports trouvés.
	 */
	List<RapportPrestationImposable> getRapportsPrestationImposable(Long numeroDebiteur, Long numeroSourcier, boolean activesOnly, boolean doNotAutoFlush);

	/**
	 * Compte le nombre de rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur un numéro de débiteur
	 * @param activesOnly    <b>vrai</b> s'il ne faut tenir compte que des rapports actifs à l'heure actuelle; <b>faux</b> pour tenir compte de tous les rapports existants.
	 * @return le nombre de rapports trouvés
	 */
	int countRapportsPrestationImposable(Long numeroDebiteur, boolean activesOnly);

	/**
	 * Recherche les rapports qui pointent vers le tiers spécifié.
	 *
	 * @param tiersId                   l'id du tiers
	 * @param showHisto                 <code>true</code> si les rapports terminés/annulés sont concernés
	 * @param types                     les types de rapports entre tiers à récupérer
	 * @param fullList                  boolean qui dfinit si on veut la liste entière ou une liste partielle en fonction de la pagination
	 * @return la liste des rapports trouvés
	 */
	List<RapportEntreTiers> findBySujetAndObjet(long tiersId, boolean showHisto, Set<RapportEntreTiersKey> types, ParamPagination pagination, boolean fullList);

	/**
	 * Compte le nombre de rappors qui pointent vers le tiers spécifié.  Note : les rapports de type 'contact impôt source' et 'prestation imposable' sont comptés en fonction du type de tiers.
	 *
	 * @param tiersId                    l'id du sujet
	 * @param showHisto                 <code>true</code> si les rapports terminés/annulés sont concernés
	 * @param types                     les types de rapports entre tiers à récupérer
	 * @return le nombre de rapports qui pointent vers le sujet spécifié.
	 */
	int countBySujetAndObjet(long tiersId, boolean showHisto, Set<RapportEntreTiersKey> types);

	/**
	 * Efface (réellement !) tous les rapports entre tiers d'un type donné
	 * @param kind le type des rapports entre tiers à effacer
	 * @return le nombre de rapports effacés
	 */
	int removeAllOfKind(TypeRapportEntreTiers kind);

	/**
	 * Récupère tous les couples (sujet/objet) de tiers qui possèdent plusieurs rapports entre tiers non-annulés du type indiqué (quelles que soient les dates !)
	 * @param kind le type des rapports entre tiers dont on cherche les doublons
	 * @return une liste non nulle de paires (sujet/objet) qui possèdent plusieurs rapports entre tiers non-annulés du type indiqué
	 */
	List<Pair<Long, Long>> getDoublonsCandidats(TypeRapportEntreTiers kind);
}
