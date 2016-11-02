package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
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
	 * Renvoie pour une organisation la liste triées des événements ayant une date de valeur postérieure à la date fournie. Les événements
	 * qui sont marqués en base comme annulés ne sont pas pris en compte.
	 *
	 * @param noOrganisation le numéro de l'organisation visée
	 * @param date la date de référence
	 * @return la liste des événements postérieurs à la date
	 */
	@NotNull
	List<EvenementOrganisation> getEvenementsOrganisationApresDateNonAnnules(Long noOrganisation, RegDate date);

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

	/**
	 * <p>
	 *     Détermine si l'évnement passé en paramètre possède une date de valeur antérieure à la date de valeur de tout autre
	 *     événement reçu avant. (Evénement dans le passé)
	 * </p>
	 *
	 * <p>
	 *     Note: cela veut dire qu'un événement dans le passé pour le même jour date de valeur ne sera pas détecté, car la comparaison
	 *     se fait sur le jour.
	 * </p>
	 *
	 * @param event l'événement à vérifer
	 * @return <code>true</code> si l'événement a été reçu pour une date de valeur dans le passé par rapport à d'autres événements déjà reçus. <code>false</code> sinon.
	 */
	boolean isEvenementDateValeurDansLePasse(EvenementOrganisation event);
}
