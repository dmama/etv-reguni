package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;

public interface EvenementRFImportDAO extends GenericDAO<EvenementRFImport, Long> {

	/**
	 * @param type le type d'import considéré
	 * @return le prochain import à processé en respectant l'état de processing des rapports et l'ordre chronologique de traitement.
	 */
	@Nullable
	EvenementRFImport findNextImportToProcess(TypeImportRF type);

	/**
	 * @param importId l'id de l'import courant (les éventuelles mutations non-traitées de cet import seront ignorées)
	 * @param type     le type d'import considéré
	 * @return retourne l'id de l'import le plus anciens qui possède encore des mutations à traiter (A_TRAITER ou EN_ERREUR)
	 */
	@Nullable
	EvenementRFImport findOldestImportWithUnprocessedMutations(long importId, TypeImportRF type);

	/**
	 * @param importId l'id de l'import courant
	 * @param type     le type d'import considéré
	 * @return la date de valeur de l'import le plus ancien ayant été traité (complétement ou partiellement) sans tenir compte de l'import spécifié.
	 */
	@Nullable
	RegDate findValueDateOfOldestProcessedImport(long importId, TypeImportRF type);

	/**
	 * Recherche des événements d'import qui correspondent aux critères spécifiés.
	 *
	 * @param etats      le ou les états des événements (optionnel)
	 * @param pagination le numéro de page
	 * @return les événements d'import correspondants
	 */
	List<EvenementRFImport> find(@Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination);

	/**
	 * @param etats le ou les états des événements (optionnel)
	 * @return le nombre d'événements qui correspondent aux critères spécifiés.
	 */
	int count(@Nullable List<EtatEvenementRF> etats);

	/**
	 * Rattrape d'éventuels rapport d'importation interrompus par crash (ou kill) de la JVM en cours de traitement.
	 *
	 * @return le nombre de rapport corrigés.
	 */
	int fixAbnormalJVMTermination();
}
