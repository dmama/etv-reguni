package ch.vd.uniregctb.listes.afc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.listes.afc.pm.ExtractionDonneesRptPMResults;
import ch.vd.uniregctb.listes.afc.pm.ModeExtraction;
import ch.vd.uniregctb.listes.afc.pm.VersionWS;

/**
 * Interface du service utilisé par l'extraction des listes des données de référence RPT
 */
public interface ExtractionDonneesRptService {

	/**
	 * Extrait la liste des données de référence RPT PP de la période fiscale donnée
	 * @param dateTraitement date d'exécution de l'extraction
	 * @param pf période fiscale de référence
	 * @param mode type d'extraction à effectuer
	 * @param nbThreads degrés de parallélisation du traitement
	 * @return extraction
	 */
	ExtractionDonneesRptResults produireExtraction(RegDate dateTraitement, int pf, TypeExtractionDonneesRpt mode, int nbThreads, StatusManager statusManager);

	/**
	 * Extrait la liste des données de référence RPT PP de la période fiscale donnée
	 * @param dateTraitement date d'exécution de l'extraction
	 * @param pf période fiscale de référence
	 * @param versionWS version du WS à utiliser pour le mapping des constantes énumérées
	 * @param mode mode d'extraction
	 * @param nbThreads niveau de parallélisation du traitement
	 * @param statusManager status manager
	 * @return résultats de l'extraction
	 */
	ExtractionDonneesRptPMResults produireExtractionIBC(RegDate dateTraitement, int pf, VersionWS versionWS, ModeExtraction mode, int nbThreads, StatusManager statusManager);
}
