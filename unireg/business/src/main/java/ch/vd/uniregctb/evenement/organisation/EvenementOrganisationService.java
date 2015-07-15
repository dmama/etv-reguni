package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public interface EvenementOrganisationService {

	/**
	 * Interroge le DAO des événements civils pour construire une collection des événements à traiter pour l'individu donné
     *
	 * @param noIndividu numéro de l'individu civil dont on cherche les événements à traiter
     *
	 * @return une liste des informations autour des événements à traiter
	 */
	List<EvenementOrganisationBasicInfo> buildLotEvenementsOrganisationNonTraites(long noIndividu);

    /**
     *
     * @param id id de l'événement à retrouver
     *
     * @return l'evenement organisation correspondant à l'id
     */
    EvenementOrganisation get(Long id);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @param pagination paramètres de pagination
     *
     * @return La liste des evenements satisfaisant les critères de recherche et la pagination
     */
    List<EvenementOrganisation> find(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, ParamPagination pagination);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @return le nombre d'événements satisfaisant les critères de recherche
     */
    int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion);

    /**
     * Force un évenement
     *
     * @param id id de l'événement à forcer
     */
    void forceEvenement(long id);

}
