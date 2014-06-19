package ch.vd.uniregctb.evenement.reqdes;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

public interface ReqDesManager {

	/**
	 * @param criteria critères de recherche des unités de traitement
	 * @param pagination informations de pagination à utiliser pour le résultat
	 * @return la liste des unités de traitement correspondant aux critères et à la pagination voulue
	 */
	List<ReqDesUniteTraitementListView> find(ReqDesCriteriaView criteria, ParamPagination pagination);

	/**
	 * @param criteria critères de recherche des unités de traitement
	 * @return le nombre d'unités de traitement qui correspondent, en tout, aux critères
	 */
	int count(ReqDesCriteriaView criteria);

	/**
	 * @param idUniteTraitement l'identifiant de l'unité de traitement à visualiser
	 * @return les données détaillées de cette unité de traitement
	 */
	ReqDesUniteTraitementDetailedView get(long idUniteTraitement);
}
