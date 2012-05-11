package ch.vd.uniregctb.evenement.civil.ech;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import java.util.List;

public interface EvenementCivilEchService {

	/**
	 * Interroge le DAO des événements civils pour construire une collection des événements à traiter pour l'individu donné
     *
	 * @param noIndividu numéro de l'individu civil dont on cherche les événements à traiter
     *
	 * @return une liste des informations autour des événements à traiter
	 */
	List<EvenementCivilEchBasicInfo> buildLotEvenementsCivils(long noIndividu);

    /**
     * retrouve dans le civil le numéro d'individu concerné par l'événement
     *
     * @param event l'événement dont le numéro d'individu doit être retrouvé
     *
     * @return le numéro de l'individu concerné par l'évenemnt
     *
     * @throws EvenementCivilException
     */
    long getNumeroIndividuPourEvent(EvenementCivilEch event) throws EvenementCivilException;

    /**
     * Assigne un numéro d'individu donné à un evenement donné.
     * Si
     *
     * @param event evenement pour lequel on veut assigné le numéro d'individu
     *
     * @param numeroIndividu le numéro d'individu a assigner
     *
     * @return l'événement mise à jour
     */
    EvenementCivilEch assigneNumeroIndividu(EvenementCivilEch event, long numeroIndividu);

    /**
     *
     * @param id id de l'événement à retrouver
     *
     * @return l'evenement civil correspondant à l'id
     */
    EvenementCivilEch get(Long id);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @param pagination paramètres de pagination
     *
     * @return La liste des evenements satisfaisant les critères de recherche et la pagination
     */
    List<EvenementCivilEch> find(EvenementCivilCriteria<TypeEvenementCivilEch> criterion, ParamPagination pagination);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @return le nombre d'événements satisfaisant les critères de recherche
     */
    int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion);

    /**
     * Force un évenement
     *
     * @param id id de l'événement à forcer
     */
    void forceEvenement(Long id);

}
