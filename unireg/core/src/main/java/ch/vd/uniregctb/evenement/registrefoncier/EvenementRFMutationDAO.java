package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementRFMutationDAO extends GenericDAO<EvenementRFMutation, Long> {

	/**
	 * Recherche les mutations d'un import qui sont dans un ou plusieurs états.
	 *
	 * @param importId   l'id de l'import considéré
	 * @param typeEntite filtre sur les types de mutations
	 * @param etats      un ou plusieurs états  @return les ids des mutations correspondantes.
	 */
	@NotNull
	List<Long> findIds(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull EtatEvenementRF... etats);

	/**
	 * Recherche une mutation pour un import, un immeuble et un type d'entité.
	 *
	 * @param importId     l'id de l'import considéré
	 * @param typeEntite   filtre sur les types de mutations
	 * @param idImmeubleRF l'id RF de l'immeuble considéré
	 * @return zéro ou une mutation.
	 */
	@Nullable
	EvenementRFMutation find(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull String idImmeubleRF);

	/**
	 * Passe à l'état FORCE toutes les mutations non traitées de l'import spécifié.
	 * @param importId     l'id de l'import
	 * @return le nombre de mutations modifiées.
	 */
	int forceMutations(long importId);
}
