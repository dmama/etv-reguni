package ch.vd.unireg.reqdes;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.common.pagination.ParamPagination;

/**
 * Interface d'accès aux objets persistés de type {@link UniteTraitement}
 */
public interface UniteTraitementDAO extends GenericDAO<UniteTraitement, Long> {

	/**
	 * Récupère les unités de traitement qui correspondent aux critères et à la pagination demandée
	 * @param criteria critères de recherche
	 * @param pagination pagination à respected
	 * @return la liste des unités de traitement trouvées
	 */
	List<UniteTraitement> find(UniteTraitementCriteria criteria, ParamPagination pagination);

	/**
	 * @param criteria critères de recherche
	 * @return le nombre d'unités de traitement qui correspondent, en tout, aux critères
	 */
	int getCount(UniteTraitementCriteria criteria);
}
