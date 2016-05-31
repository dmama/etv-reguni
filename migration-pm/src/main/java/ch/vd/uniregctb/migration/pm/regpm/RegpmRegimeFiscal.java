package ch.vd.uniregctb.migration.pm.regpm;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface implémentée pour tous les Regimes Fiscaux de RegPM
 */
public interface RegpmRegimeFiscal {

	/**
	 * @return date de début de validité
	 */
	RegDate getDateDebut();

	/**
	 * @return date d'annulation, évidemment optionnelle
	 */
	RegDate getDateAnnulation();

	/**
	 * @return le type du régime fiscal
	 */
	RegpmTypeRegimeFiscal getType();
}
