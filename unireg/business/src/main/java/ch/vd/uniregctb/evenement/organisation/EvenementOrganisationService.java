package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public interface EvenementOrganisationService {

	/**
	 * Interroge le DAO des événements organisation pour construire une collection des événements à traiter pour l'organisation donnée
     *
	 * @param noOrganisation numéro de l'organisation dont on cherche les événements à traiter
     *
	 * @return une liste des informations autour des événements à traiter
	 */
	List<EvenementOrganisationBasicInfo> buildLotEvenementsOrganisationNonTraites(long noOrganisation);

    List<EvenementOrganisation> getEvenementsNonTraitesOrganisation(long noOrganisation);

    /**
     *
     * @param id id de l'événement à retrouver
     *
     * @return l'evenement organisation correspondant à l'id
     */
    EvenementOrganisation get(Long id);

    /**
     * Retourne la liste des événements concernant une organisation
     * @param noOrganisation L'organisation concernée
     * @return la liste ordonnées chronologiquement. Vide si aucun événement.
     */
    List<EvenementOrganisation> getEvenementsOrganisation(Long noOrganisation);

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

}
