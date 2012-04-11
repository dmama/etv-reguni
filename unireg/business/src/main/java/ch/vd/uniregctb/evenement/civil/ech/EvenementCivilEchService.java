package ch.vd.uniregctb.evenement.civil.ech;

import java.util.List;

public interface EvenementCivilEchService {

	/**
	 * Interroge le DAO des événements civils pour construire une collection des événements à traiter pour l'individu donné
	 * @param noIndividu numéro de l'individu civil dont on cherche les événements à traiter
	 * @return une liste des informations autour des événements à traiter
	 */
	List<EvenementCivilEchBasicInfo> buildLotEvenementsCivils(long noIndividu);
}
