package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

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
}
