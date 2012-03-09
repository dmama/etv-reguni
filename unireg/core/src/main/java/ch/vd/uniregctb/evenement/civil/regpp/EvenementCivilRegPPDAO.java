package ch.vd.uniregctb.evenement.civil.regpp;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * DAO des événements civils externes.
 *
 */
public interface EvenementCivilRegPPDAO extends GenericDAO<EvenementCivilRegPP, Long> {

	/**
	 * Recherche la liste des événements civils rattaché à l'individu pour une date et un type d'événement donné,
	 * et dont l'état ne correspond pas à un état traité (TRAITE, A VERIFIER)
	 *
	 * @param dateEvenement		date de l'événement recherché
	 * @param typeEvenement		type de l'événement recherché
	 * @param noIndividu		numéro de l'individu
	 *
	 * @return  la liste des événements civils rattaché à l'individu pour une date et un type d'événement donné.
	 */
	List<EvenementCivilRegPP> rechercheEvenementExistantEtTraitable(RegDate dateEvenement, TypeEvenementCivil typeEvenement, Long noIndividu );

	List<EvenementCivilRegPP> find(EvenementCivilCriteria <TypeEvenementCivil> criterion, ParamPagination paramPagination);

	int count(EvenementCivilCriteria<TypeEvenementCivil> criterion);

	/**
	 * Récupère la liste des ids des événements civils dont le statut est A_TRAITE ou EN_ERREUR.
	 *
	 * @return une liste d'ids des événements civils
	 */
	List<Long> getEvenementCivilsNonTraites();

	/**
	 * Récupère la liste des ids des événements civils dont le statut est A_TRAITE ou EN_ERREUR pour les individus
	 * dont le numéro est donné en paramètre.
	 *
	 * @param nosIndividus ...
	 *
	 * @return une liste d'ids des événements civils
	 */
	List<EvenementCivilRegPP> getEvenementsCivilsNonTraites(Collection<Long> nosIndividus);

	/**
	 * Récupère la liste des ids des événements civils dont le statut est en erreur pour un individu donné
	 * @param numIndividu le numéro de l'individu principal de l^événement
	 * @return une liste d'ids des événements civils
	 */
	List<Long> getIdsEvenementCivilsErreurIndividu(Long numIndividu);

	/**
	 * Recherche et retourne les événements qui référence un individu donné.
	 *
	 * @param numIndividu un numéro d'individu
	 * @return une liste d'événements
	 */
	List<EvenementCivilRegPP> findEvenementByIndividu(Long numIndividu);
}
