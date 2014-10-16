package ch.vd.uniregctb.reqdes;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementReqDesDAO extends GenericDAO<EvenementReqDes, Long> {

	/**
	 * Retrouve l'éventuel entité dont le numéro de minute est donné
	 * @param noMinute numéro de minute d'un acte authentique
	 * @param visaNotaire le visa du notaire émetteur de l'acte
	 * @return les événements déjà présent en base pour ce numéro, ou <code>null</code> s'il n'y en a pas
	 */
	List<EvenementReqDes> findByNumeroMinute(String noMinute, String visaNotaire);
}
