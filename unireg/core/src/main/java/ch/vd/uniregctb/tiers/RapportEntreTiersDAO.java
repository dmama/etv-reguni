package ch.vd.uniregctb.tiers;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;

public interface RapportEntreTiersDAO extends GenericDAO<RapportEntreTiers, Long> {

	/**
	 * @param noTiersTuteur
	 * @param noTiersPupille
	 * @return
	 */
	List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille);

	/**
	 * @param noTiersTuteur
	 * @param noTiersPupille
	 * @param doNotAutoFlush
	 * @return
	 */
	List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille, boolean doNotAutoFlush);

	/**
	 * Retourne les rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur  un numéro de débiteur
	 * @param paramPagination des éventuels paramètres de pagination
	 * @param activesOnly     <b>vrai</b> s'il ne faut retourner que les rapports actifs à l'heure actuelle; <b>faux</b> pour retourner tous les rapports existants.
	 * @return les rapports trouvés.
	 */
	public List<RapportPrestationImposable> getRapportsPrestationImposable(Long numeroDebiteur, ParamPagination paramPagination, boolean activesOnly);

	/**
	 * Compte le nombre de rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur un numéro de débiteur
	 * @param activesOnly    <b>vrai</b> s'il ne faut tenir compte que des rapports actifs à l'heure actuelle; <b>faux</b> pour tenir compte de tous les rapports existants.
	 * @return le nombre de rapports trouvés
	 */
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean activesOnly);
}
