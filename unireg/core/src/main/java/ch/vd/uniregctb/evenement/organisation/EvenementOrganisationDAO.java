package ch.vd.uniregctb.evenement.organisation;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * DAO des événements organisation
 */
public interface EvenementOrganisationDAO extends GenericDAO<EvenementOrganisation, Long> {

	/**
	 * Renvoie l'ensemble des événements non encore traités (i.e. dont l'état n'est pas final) pour l'organisation donnée
	 * @param noOrganisation numéro de l'organisation sur laquelle les événements doivent être recherchés
	 * @return une liste des événements liés à l'organisation donnée et dont l'état n'est pas final (ordre non garanti)
	 */
	List<EvenementOrganisation> getEvenementsOrganisationNonTraites(long noOrganisation);

	/**
	 * Renvoie l'ensemble des événements pour l'organisation donnée
	 * @param noOrganisation numéro de l'organisation sur lequel les événements doivent être recherchés
	 * @return une liste des événements liés à l'organisation donnée et (ordre non garanti)
	 */
	List<EvenementOrganisation> getEvenementsOrganisation(long noOrganisation);

	/**
	 * Renvoie l'ensemble des événements organisation à relancer, i.e. qui sont dans l'état {@link EtatEvenementOrganisation#A_TRAITER A_TRAITER}
	 * @return la liste des événements organisation à relancer
	 */
	List<EvenementOrganisation> getEvenementsOrganisationARelancer();

	/**
	 * @return l'ensemble des identifiants des organisations pour lesquels il existe au moins un événement dans l'état {@link EtatEvenementOrganisation#EN_ATTENTE EN_ATTENTE}
	 * ou {@link EtatEvenementOrganisation#EN_ERREUR EN_ERREUR}
	 */
	Set<Long> getOrganisationsConcerneesParEvenementsPourRetry();

	/**
	 * Cherche les evenements correspondant aux criteres
	 *
	 * @param criterion critères de recherche
	 * @param paramPagination info de pagination pour la requête
	 * @return la liste d'evenements correspondant aux critères
	 */
	List<EvenementOrganisation> find(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, @Nullable ParamPagination paramPagination);

	/**
	 * Renvoie le ou les événements organisation en base Unireg émanant d'un même événement organisation.
	 *
	 * Note: On trouve plusieurs événement en base Unireg pour un seul événement RCEnt lorsque plusieurs organisations sont visées par cet événement. En effet,
	 * on crée à la réception autant d'événements que nécessaire pour respecter la cardinalité d'un événement pour une organisation, car tous les
	 * traitements sont construits sur cette hypothèse.
	 *
	 * @param noEvenement Le numéro de l'événement organisation d'origine
	 * @return La liste contenant le ou les événements résultants, tels qu'enregistrés en base
	 */
	List<EvenementOrganisation> getEvenementsForNoEvenement(long noEvenement);

	/**
	 * @param criterion les critères de recherche
	 * @return le nombre d'evenement correspondant aux critères de recherche
	 */
	int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion);
}
