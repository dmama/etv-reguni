package ch.vd.uniregctb.reqdes;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementReqDesDAO extends GenericDAO<EvenementReqDes, Long> {

	/**
	 * Retrouve l'éventuel entité dont le numéro de minute est donné
	 * @param noMinute numéroi de minute d'un acte authentique
	 * @return l'événement déjà présent en base pour ce numéro, ou <code>null</code> s'il n'y en a pas
	 */
	EvenementReqDes findByNumeroMinute(long noMinute);
}
