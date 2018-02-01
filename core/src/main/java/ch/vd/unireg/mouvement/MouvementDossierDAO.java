package ch.vd.unireg.mouvement;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.ParamSorting;

public interface MouvementDossierDAO extends GenericDAO<MouvementDossier, Long> {

	/**
	 * Recherche tous les mouvements en fonction du numero de contribuable
	 *
	 * @param numero
	 * @param seulementTraites si levé, signifie que seuls les mouvements traités seront remontés
	 * @param inclureMouvementsAnnules
	 * @return
	 */
	List<MouvementDossier> findByNumeroDossier(long numero, boolean seulementTraites, boolean inclureMouvementsAnnules);

	/**
	 * Recherche tous les mouvements de dossiers qui correspondent aux critères donnés (potentiellement avec pagination)
	 * @param criteria critères de recherche
	 * @param paramPagination si donné, pagination a utiliser, sinon tous les résultats
	 * @return Liste de mouvements trouvés
	 */
	List<MouvementDossier> find(MouvementDossierCriteria criteria, @Nullable ParamPagination paramPagination);

	/**
	 * Recherche les ID de tous les mouvements de dossiers qui correspondent aux critères donnés
	 * @param criteria critères de recherche
	 * @param sorting indication du tri à utiliser
	 * @return liste des ID des mouvements trouvés
	 */
	List<Long> findIds(MouvementDossierCriteria criteria, @Nullable ParamSorting sorting);

	/**
	 * Renvoie le nombre de mouvements de dossiers qui correspondent aux critères donnés
	 * @param criteria
	 * @return
	 */
	int count(MouvementDossierCriteria criteria);

	/**
	 * Recherche de plusieurs mouvements de dossier par ID en même temps
	 * @param ids les ID techniques des mouvements à remonter
	 * @return la liste des mouvements trouvés
	 */
	List<MouvementDossier> get(long[] ids);

	/**
	 * Retourne une liste de proto-bordereaux (= groupements de mouvements de dossiers qui pourraient
	 * aller ensemble sur un bordereau de dossier)
	 * @param noCollAdmInitiatrice si non-null, ne renvoie que les mouvements initiés par la collectivité administrative donnée
	 * @return liste des combinaisons trouvées, ou null si aucune
	 */
	List<ProtoBordereauMouvementDossier> getAllProtoBordereaux(@Nullable Integer noCollAdmInitiatrice);
}
