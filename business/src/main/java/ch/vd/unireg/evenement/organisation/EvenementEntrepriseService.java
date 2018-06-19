package ch.vd.unireg.evenement.organisation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public interface EvenementEntrepriseService {

	/**
	 * Interroge le DAO des événements entreprise pour construire une collection des événements à traiter pour l'entreprise donnée
     *
	 * @param noEntrepriseCivile numéro de l'entreprise dont on cherche les événements à traiter
     *
	 * @return une liste des informations autour des événements à traiter
	 */
	List<EvenementEntrepriseBasicInfo> buildLotEvenementsEntrepriseNonTraites(long noEntrepriseCivile);

    List<EvenementEntreprise> getEvenementsNonTraitesEntreprise(long noEntrepriseCivile);

	/**
	 * Renvoie pour une entreprise la liste triées des événements ayant une date de valeur postérieure à la date fournie. Les événements
	 * qui sont marqués en base comme annulés ne sont pas pris en compte.
	 *
	 * @param noEntrepriseCivile le numéro de l'entreprise visée
	 * @param date la date de référence
	 * @return la liste des événements postérieurs à la date
	 */
	@NotNull
	List<EvenementEntreprise> getEvenementsEntrepriseApresDateNonAnnules(Long noEntrepriseCivile, RegDate date);

	/**
     *
     * @param id id de l'événement à retrouver
     *
     * @return l'événement entreprise correspondant à l'id
     */
    EvenementEntreprise get(Long id);

    /**
     * Retourne la liste des événements concernant une entreprise
     * @param noEntrepriseCivile L'entreprise concernée
     * @return la liste ordonnées chronologiquement. Vide si aucun événement.
     */
    List<EvenementEntreprise> getEvenementsEntreprise(Long noEntrepriseCivile);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @param pagination paramètres de pagination
     *
     * @return La liste des evenements satisfaisant les critères de recherche et la pagination
     */
    List<EvenementEntreprise> find(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion, ParamPagination pagination);

    /**
     *
     * @param criterion les critères de recherche
     *
     * @return le nombre d'événements satisfaisant les critères de recherche
     */
    int count(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion);

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
	boolean isEvenementDateValeurDansLePasse(EvenementEntreprise event);

	/**
	 * @param date date de valeur des événements
	 * @param noEntrepriseCivile no de l'entreprise
	 * @return la liste des événements non annulés valables à cette date pour l'entreprise, triés dans l'ordre de réception.
	 */
	@NotNull
	List<EvenementEntreprise> evenementsPourDateValeurEtEntreprise(RegDate date, Long noEntrepriseCivile);

	/**
	 * Renvoie l'événement entreprise en base Unireg correspondant au numéro d'annonce IDE.
	 *
	 * @param noAnnonce Le numéro de l'annonce à l'IDE
	 * @return L'événement correspondant, ou <code>null</code> si aucune ne correspond
	 */
	EvenementEntreprise getEvenementForNoAnnonceIDE(long noAnnonce);
}
