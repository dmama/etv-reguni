package ch.vd.unireg.tiers.dao;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.tiers.Remarque;

public interface RemarqueDAO extends GenericDAO<Remarque, Long> {

	/**
	 * Retourne les remarques associées à un tiers
	 *
	 * @param tiersId
	 * @return les remarques associées au tiers
	 */
	List<Remarque> getRemarques(Long tiersId);
}
