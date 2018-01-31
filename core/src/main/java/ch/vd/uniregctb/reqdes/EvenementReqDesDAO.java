package ch.vd.uniregctb.reqdes;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface EvenementReqDesDAO extends GenericDAO<EvenementReqDes, Long> {

	/**
	 * Retrouve les éventuelles entités dont le numéro de minute est donné
	 * @param noMinute numéro de minute d'un acte authentique
	 * @param visaNotaire le visa du notaire émetteur de l'acte
	 * @return les événements déjà présents en base pour ce numéro
	 */
	List<EvenementReqDes> findByNumeroMinute(String noMinute, String visaNotaire);

	/**
	 * Retrouve les éventuelles entités dont le numéro d'affaire (= identifiant ReqDes) est donné
	 * @param noAffaire numéro d'affaire fourni
	 * @return les événements déjà présents en base pour ce numéro
	 */
	List<EvenementReqDes> findByNoAffaire(long noAffaire);
}
