package ch.vd.unireg.evenement.entreprise;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

/**
 * DAO des événements entreprise
 */
public interface EvenementEntrepriseDAO extends GenericDAO<EvenementEntreprise, Long> {

	/**
	 * Renvoie l'ensemble des événements non encore traités (i.e. dont l'état n'est pas final) pour l'entreprise donnée
	 *
	 * @param noEntrepriseCivile numéro de l'entreprise sur laquelle les événements doivent être recherchés
	 * @return une liste des événements liés à l'entreprise donnée et dont l'état n'est pas final (ordre non garanti)
	 */
	List<EvenementEntreprise> getEvenementsNonTraites(long noEntrepriseCivile);

	/**
	 * Renvoie l'ensemble des événements pour l'entreprise donnée
	 *
	 * @param noEntrepriseCivile numéro de l'entreprise sur lequel les événements doivent être recherchés
	 * @return une liste des événements liés à l'entreprise donnée et (ordre non garanti)
	 */
	List<EvenementEntreprise> getEvenements(long noEntrepriseCivile);

	/**
	 * Renvoie l'ensemble des événements entreprise à relancer, i.e. qui sont dans l'état {@link EtatEvenementEntreprise#A_TRAITER A_TRAITER}
	 *
	 * @return la liste des événements entreprise à relancer
	 */
	List<EvenementEntreprise> getEvenementsARelancer();

	/**
	 * Renvoie pour une entreprise la liste triées des événements ayant une date de valeur postérieure à la date fournie. Les événements qui sont marqués en base comme annulés ne sont pas pris en compte.
	 *
	 * @param noEntrepriseCivile le numéro de l'entreprise visée
	 * @param date               la date de référence
	 * @return la liste des événements postérieurs à la date
	 */
	@NotNull
	List<EvenementEntreprise> getEvenementsApresDateNonAnnules(Long noEntrepriseCivile, RegDate date);

	/**
	 * @return l'ensemble des identifiants des entreprises pour lesquels il existe au moins un événement dans l'état {@link EtatEvenementEntreprise#EN_ATTENTE EN_ATTENTE} ou {@link EtatEvenementEntreprise#EN_ERREUR EN_ERREUR}
	 */
	Set<Long> getNosEntreprisesCivilesConcerneesParEvenementsPourRetry();

	/**
	 * Cherche les evenements correspondant aux criteres
	 *
	 * @param criterion       critères de recherche
	 * @param paramPagination info de pagination pour la requête
	 * @return la liste d'evenements correspondant aux critères
	 */
	List<EvenementEntreprise> find(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion, @Nullable ParamPagination paramPagination);

	/**
	 * Renvoie le ou les événements entreprise en base Unireg émanant d'un même événement entreprise.
	 * <p>
	 * Note: On trouve plusieurs événement en base Unireg pour un seul événement RCEnt lorsque plusieurs entreprises sont visées par cet événement. En effet, on crée à la réception autant d'événements que nécessaire pour respecter la cardinalité
	 * d'un événement pour une entreprise, car tous les traitements sont construits sur cette hypothèse.
	 *
	 * @param noEvenement Le numéro de l'événement entreprise d'origine
	 * @return La liste contenant le ou les événements résultants, tels qu'enregistrés en base
	 */
	List<EvenementEntreprise> getEvenementsForNoEvenement(long noEvenement);

	/**
	 * Renvoie l'événement entreprise en base Unireg correspondant au numéro d'annonce IDE.
	 *
	 * @param noAnnonce Le numéro de l'annonce à l'IDE
	 * @return L'événement correspondant, ou <code>null</code> si aucune ne correspond
	 */
	EvenementEntreprise getEvenementForNoAnnonceIDE(long noAnnonce);

	/**
	 * Renvoie le ou les événements entreprise en base Unireg émanant d'un même message expédié par RCEnt (par businessId).
	 * <p>
	 * Note: On trouve plusieurs événement en base Unireg pour un seul événement RCEnt lorsque plusieurs entreprises sont visées par cet événement. En effet, on crée à la réception autant d'événements que nécessaire pour respecter la cardinalité
	 * d'un événement pour une entreprise, car tous les traitements sont construits sur cette hypothèse.
	 *
	 * @param businessId L'identifiant du message ESB d'origine
	 * @return La liste contenant le ou les événements résultants, tels qu'enregistrés en base
	 */
	List<EvenementEntreprise> getEvenementsForBusinessId(String businessId);

	/**
	 * @param criterion les critères de recherche
	 * @return le nombre d'evenement correspondant aux critères de recherche
	 */
	int count(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion);

	/**
	 * <p>
	 * Détermine si l'évnement passé en paramètre possède une date de valeur antérieure à la date de valeur de tout autre événement reçu avant. (Evénement dans le passé)
	 * </p>
	 *
	 * <p>
	 * Note: cela veut dire qu'un événement dans le passé pour le même jour date de valeur ne sera pas détecté, car la comparaison se fait sur le jour.
	 * </p>
	 *
	 * @param event l'événement à vérifer
	 * @return <code>true</code> si l'événement a été reçu pour une date de valeur dans le passé par rapport à d'autres événements déjà reçus. <code>false</code> sinon.
	 */
	boolean isEvenementDateValeurDansLePasse(EvenementEntreprise event);

	/**
	 * @param date               date de valeur des événements
	 * @param noEntrepriseCivile no de l'entreprise
	 * @return la liste des événements non annulés valables à cette date pour l'entreprise
	 */
	@NotNull
	List<EvenementEntreprise> evenementsPourDateValeurEtEntreprise(RegDate date, Long noEntrepriseCivile);

}
