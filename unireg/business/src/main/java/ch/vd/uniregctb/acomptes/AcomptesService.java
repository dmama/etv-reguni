package ch.vd.uniregctb.acomptes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;

public interface AcomptesService {

	/**
	 * Retourne les données nécessaires à la génération des populations pour les bases acomptes
	 * @param dateTraitement
	 * @param nbThreads
	 * @param annee
	 * @param statusManager
	 * @return les données pour la liste globale
	 */
	public AcomptesResults produireAcomptes(RegDate dateTraitement, int nbThreads, Integer annee, StatusManager statusManager);


}
