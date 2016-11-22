package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;

public interface EvenementRFImportDAO extends GenericDAO<EvenementRFImport, Long> {

	/**
	 * @return le prochain import à processé en respectant l'état de processing des rapports et l'ordre chronologique de traitement.
	 */
	@Nullable
	EvenementRFImport findNextImportToProcess();

	/**
	 * @param importId l'id de l'import courant (les éventuelles mutations non-traitées de cet import seront ignorées)
	 * @return retourne l'id de l'import le plus anciens qui possède encore des mutations à traiter (A_TRAITER ou EN_ERREUR)
	 */
	@Nullable
	EvenementRFImport findOldestImportWithUnprocessedMutations(long importId);

	/**
	 * Recherche des événements d'import qui correspondent aux critères spécifiés.
	 *
	 * @param etats       le ou les états des événements (optionnel)
	 * @param pagination le numéro de page
	 * @return les événements d'import correspondants
	 */
	List<EvenementRFImport> find(@Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination);

	/**
	 * @param etats       le ou les états des événements (optionnel)
	 * @return le nombre d'événements qui correspondent aux critères spécifiés.
	 */
	int count(@Nullable List<EtatEvenementRF> etats);
}
