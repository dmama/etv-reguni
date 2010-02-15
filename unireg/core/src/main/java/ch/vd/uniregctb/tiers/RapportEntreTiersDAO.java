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
	 * @param numeroDebiteur
	 * @param paramPagination
	 * @return
	 */
	public List<RapportPrestationImposable> getRapportsPrestationImposable(Long numeroDebiteur, ParamPagination paramPagination) ;

	/**
	 * Compte le nombre de rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur
	 * @return
	 */
	public int countRapportsPrestationImposable(Long numeroDebiteur);
}
