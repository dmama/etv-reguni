package ch.vd.unireg.tiers.dao;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.tiers.DecisionAci;

public interface DecisionAciDAO extends GenericDAO<DecisionAci, Long> {

	/**$
	 * Retourne les décisions aci associés à un tiers
	 * @param tiersId numéro du tiers pour qui on veut récupérer les décision aci
	 * @return la liste des décisions aci
	 */
	List<DecisionAci> getDecisionsAci(Long tiersId);
}
