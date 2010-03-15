package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * DAO des événements civils regroupés.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public interface EvenementCivilRegroupeDAO extends GenericDAO<EvenementCivilRegroupe, Long> {

	/**
	 * Recherche la liste des événements regroupés rattaché à l'individu pour une date et un type d'événement donné,
	 * et dont l'état ne correspond pas à un état traité (TRAITE, A VERIFIER)
	 *
	 * @param dateEvenement		date de l'événement recherché
	 * @param typeEvenement		type de l'événement recherché
	 * @param noIndividu		numéro de l'individu
	 *
	 * @return  la liste des événements regroupés rattaché à l'individu pour une date et un type d'événement donné.
	 */
	List<EvenementCivilRegroupe> rechercheEvenementExistant(RegDate dateEvenement, TypeEvenementCivil typeEvenement, Long noIndividu );

	/**
	 * @param criterion
	 * @param paramPagination
	 * @return
	 */
	public List<EvenementCivilRegroupe> find(EvenementCriteria criterion, ParamPagination paramPagination);

	/**
	 * @param criterion
	 * @return
	 */
	public int count(EvenementCriteria criterion);

	/**
	 * Récupère la liste des ids des événements civils dont le statut est A_TRAITE ou EN_ERREUR.
	 *
	 * @return une liste d'ids des événements civils regroupés
	 */
	List<Long> getEvenementCivilsNonTraites();

	/**
	 * Récupère la liste des ids des événements civils dont le statut est A_TRAITE.
	 *
	 * @return une liste d'ids des événements civils regroupés
	 */
	List<Long> getIdsEvenementCivilsATraites();

	/**
	 * Récupère la liste des ids des événements civils dont le statut est en erreur pour un individu donné
	 * @param numIndividu le numéro de l'individu principal de l^événement
	 * @return une liste d'ids des événements civils regroupés
	 */
	List<Long> getIdsEvenementCivilsErreurIndividu(Long numIndividu);
}
