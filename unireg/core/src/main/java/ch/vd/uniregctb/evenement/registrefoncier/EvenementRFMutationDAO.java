package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;

public interface EvenementRFMutationDAO extends GenericDAO<EvenementRFMutation, Long> {

	/**
	 * Recherche les mutations d'un import qui sont dans un ou plusieurs états.
	 *
	 * @param importId      l'id de l'import considéré
	 * @param typeEntite    filtre sur les types de mutations
	 * @param etats         un ou plusieurs états
	 * @param typesMutation un ou plusieurs types de mutations
	 * @return les ids des mutations correspondantes.
	 */
	@NotNull
	List<Long> findIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull Collection<EtatEvenementRF> etats, @NotNull Collection<TypeMutationRF> typesMutation);

	/**
	 * Recherche le nombre de mutations d'un import donné selon certains critères.
	 *
	 * @param importId     l'id de l'import considéré
	 * @param typeEntite   filtre sur les types de mutations
	 * @param typeMutation le type de mutation considéré
	 * @return le nombre des mutations trouvées.
	 */
	long count(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation);

	/**
	 * Recherche les ids RF de mutations d'un import donné selon certains critères.
	 *
	 * @param importId     l'id de l'import considéré
	 * @param typeEntite   filtre sur les types de mutations
	 * @param typeMutation le type de mutation considéré
	 * @return une liste des IDs RF des mutations trouvées.
	 */
	@NotNull
	Iterator<String> findRfIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation);

	/**
	 * Recherche une mutation pour un import, un immeuble et un type d'entité.
	 *
	 * @param importId     l'id de l'import considéré
	 * @param typeEntite   filtre sur les types de mutations
	 * @param idImmeubleRF l'id RF de l'immeuble considéré
	 * @return zéro ou une mutation.
	 */
	@Nullable
	EvenementRFMutation find(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull String idImmeubleRF);

	/**
	 * Passe la mutation spécifiée à l'état FORCE.
	 *
	 * @param mutId l'id d'une mutation
	 * @return le nombre de mutations modifiées.
	 */
	int forceMutation(long mutId);

	/**
	 * Passe à l'état FORCE toutes les mutations non traitées de l'import spécifié.
	 *
	 * @param importId l'id de l'import
	 * @return le nombre de mutations modifiées.
	 */
	int forceMutations(long importId);

	/**
	 * @param importId l'id d'un import
	 * @return le nombre de mutations pour chaque état
	 */
	Map<EtatEvenementRF, Integer> countByState(long importId);

	/**
	 * Efface toutes les mutations associées avec l'import spécifié.
	 *
	 * @param importId   l'id d'un import
	 * @param maxResults le nombre maximal de mutations à supprimer d'un coup
	 * @return le nombre de mutations supprimées
	 */
	int deleteMutationsFor(long importId, int maxResults);

	/**
	 * @return l'id de l'import des prochaines mutations à processer, c'est-à-dire les mutations non-traitées les plus anciennes (par date de valeur de l'import).
	 */
	@Nullable
	Long findNextMutationsToProcess();

	/**
	 * Recherche des mutations d'import qui correspondent aux critères spécifiés.
	 *
	 * @param importId   l'id de l'import considéré
	 * @param etats      le ou les états des événements (optionnel)
	 * @param pagination le numéro de page
	 * @return les événements d'import correspondants
	 */
	@NotNull
	List<EvenementRFMutation> find(long importId, @Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination);

	/**
	 * @param importId l'id de l'import considéré
	 * @param etats    le ou les états des événements (optionnel)
	 * @return le nombre de mutations qui correspondent aux critères spécifiés.
	 */
	int count(long importId, @Nullable List<EtatEvenementRF> etats);
}
