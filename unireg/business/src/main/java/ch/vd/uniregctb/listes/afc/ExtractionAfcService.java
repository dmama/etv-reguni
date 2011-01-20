package ch.vd.uniregctb.listes.afc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;

/**
 * Interface du service utilisé par l'extraction des listes AFC
 */
public interface ExtractionAfcService {

	/**
	 * Extrait la liste AFC de la période fiscale donnée
	 * @param dateTraitement date d'exécution de l'extraction
	 * @param pf période fiscale de référence
	 * @param mode type d'extraction à effectuer
	 * @param nbThreads degrés de parallélisation du traitement
	 * @return extraction
	 */
	ExtractionAfcResults produireExtraction(RegDate dateTraitement, int pf, TypeExtractionAfc mode, int nbThreads, StatusManager statusManager);
}
