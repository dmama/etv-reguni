package ch.vd.uniregctb.evenement.registrefoncier;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementRFImportDAO extends GenericDAO<EvenementRFImport, Long> {

	/**
	 * @return le prochain import à processé en respectant l'état de processing des rapports et l'ordre chronologique de traitement.
	 */
	@Nullable
	EvenementRFImport findNextImportToProcess();

	/**
	 * Efface toutes les mutations associées avec l'import spécifié.
	 *
	 * @param importId   l'id d'un import
	 * @param maxResults le nombre maximal de mutations à supprimer d'un coup
	 * @return le nombre de mutations supprimées
	 */
	int deleteMutationsFor(long importId, int maxResults);
}
