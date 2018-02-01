package ch.vd.unireg.evenement.civil.ech;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.EvenementCivilCriteria;
import ch.vd.unireg.type.TypeEvenementCivilEch;

/**
 * DAO des événements civils à la sauce e-CH
 */
public interface EvenementCivilEchDAO extends GenericDAO<EvenementCivilEch, Long> {

	/**
	 * Renvoie l'ensemble des événements civils non encore traités (i.e. dont l'état n'est pas final) pour les individus donnés
	 * @param nosIndividus numéros des individus sur lesquels les événements doivent être recherchés
	 * @return une liste des événements liés à l'individu donné et dont l'état n'est pas final (ordre non garanti)
	 */
	List<EvenementCivilEch> getEvenementsCivilsNonTraites(Collection<Long> nosIndividus);

	/**
	 * Renvoie l'ensemble des événements civils pour l'individu donné
	 * @param noIndividu numéro de l'individu sur lequel les événements doivent être recherchés
	 * @param followLinks <code>true</code> s'il faut suivre les liens de référencement (y compris donc sur les événements qui n'auraient pas de numéro d'individu assigné)
	 * @return une liste des événements liés à l'individu donné et (ordre non garanti)
	 */
	List<EvenementCivilEch> getEvenementsCivilsPourIndividu(long noIndividu, boolean followLinks);

	/**
	 * Renvoie l'ensemble des événements civils à relancer, i.e. qui sont dans l'état {@link ch.vd.unireg.type.EtatEvenementCivil#A_TRAITER A_TRAITER}
	 * ou pour lesquels le numéro d'individu n'a pas encore été assigné
	 * @return la liste des événements civils à relancer
	 */
	List<EvenementCivilEch> getEvenementsCivilsARelancer();

	/**
	 * @return l'ensemble des identifiants des individus pour lesquels il existe au moins un événement civil dans l'état {@link ch.vd.unireg.type.EtatEvenementCivil#EN_ATTENTE EN_ATTENTE}
	 * ou {@link ch.vd.unireg.type.EtatEvenementCivil#EN_ERREUR EN_ERREUR}
	 */
	Set<Long> getIndividusConcernesParEvenementsPourRetry();

	/**
	 * Cherche les evenements correspondant aux criteres
	 *
	 * @param criterion critères de recherche
	 * @param paramPagination info de pagination pour la requête
	 * @return la liste d'evenements correspondant aux critères
	 */
	List<EvenementCivilEch> find(EvenementCivilCriteria<TypeEvenementCivilEch> criterion, @Nullable ParamPagination paramPagination);

	/**
	 * @param criterion les critères de recherche
	 * @return le nombre d'evenement correspondant aux critères de recherche
	 */
	int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion);
}
