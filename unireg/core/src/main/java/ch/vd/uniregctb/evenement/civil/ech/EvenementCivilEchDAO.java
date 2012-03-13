package ch.vd.uniregctb.evenement.civil.ech;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * DAO des événements civils à la sauce e-CH
 */
public interface EvenementCivilEchDAO extends GenericDAO<EvenementCivilEch, Long> {

	/**
	 * Renvoie l'ensemble des événements civils non encore traités (i.e. dont l'état n'est pas final) pour l'individu donné
	 * @param noIndividu numéro de l'individu commun à tous les événements à retourner
	 * @return une liste des événements liés à l'individu donné et dont l'état n'est pas final (ordre non garanti)
	 */
	List<EvenementCivilEch> getEvenementsCivilsNonTraites(long noIndividu);

	/**
	 * Renvoie l'ensemble des événements civils à relancer, i.e. qui sont dans l'état {@link ch.vd.uniregctb.type.EtatEvenementCivil#A_TRAITER A_TRAITER}
	 * ou pour lesquels le numéro d'individu n'a pas encore été assigné
	 * @return la liste des événements civils à relancer
	 */
	List<EvenementCivilEch> getEvenementsCivilsARelancer();

	/**
	 * @return l'ensemble des identifiants des individus pour lesquels il existe au moins un événement civil dans l'état {@link ch.vd.uniregctb.type.EtatEvenementCivil#EN_ATTENTE EN_ATTENTE}
	 * ou {@link ch.vd.uniregctb.type.EtatEvenementCivil#EN_ERREUR EN_ERREUR}
	 */
	Set<Long> getIndividusConcernesParEvenementsPourRetry();

	/**
	 * Cherche les evenements correspondant aux criteres
	 *
	 * @param criterion critères de recherche
	 * @param paramPagination info de pagination pour la requête
	 * @return la liste d'evenements correspondant aux critères
	 */
	public List<EvenementCivilEch> find(EvenementCivilCriteria<TypeEvenementCivilEch> criterion, ParamPagination paramPagination);

	/**
	 * @param criterion les critères de recherche
	 * @return le nombre d'evenement correspondant aux critères de recherche
	 */
	int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion);
}
