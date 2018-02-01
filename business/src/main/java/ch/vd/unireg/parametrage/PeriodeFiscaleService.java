package ch.vd.unireg.parametrage;

import ch.vd.unireg.declaration.PeriodeFiscale;

public interface PeriodeFiscaleService {

	/**
	 * Initialisation d'une nouvelle période fiscale (= la suivante !)
	 * @return la nouvelle période fiscale
	 */
	PeriodeFiscale initNouvellePeriodeFiscale();

	/**
	 * Recopie des paramètres (adaptés) de la période fiscale source à la période fiscale destination
	 * @param source période fiscale avec les paramètres à recopier
	 * @param destination période fiscale destination de la recopie
	 */
	void copyParametres(PeriodeFiscale source, PeriodeFiscale destination);

	/**
	 * Recopie des modèles de documents de la période fiscale source à la période fiscale destination
	 * @param source période fiscale avec les modèles à recopier
	 * @param destination période fiscale destination de la recopie
	 */
	void copyModelesDocuments(PeriodeFiscale source, PeriodeFiscale destination);

	/**
	 * @param annee une année
	 * @return la période fiscale correspondant à cette année-là
	 */
	PeriodeFiscale get(int annee);
}
