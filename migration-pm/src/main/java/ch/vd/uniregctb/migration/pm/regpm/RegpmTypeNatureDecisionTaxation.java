package ch.vd.uniregctb.migration.pm.regpm;

/**
 * La nature d'une décision de taxation PM
 */
public enum RegpmTypeNatureDecisionTaxation {

	/**
	 * La décision est définitive
	 */
	DEFINITIVE,

	/**
	 * La décision n'est encore que provisoire
	 */
	PROVISOIRE,

	/**
	 * La décision est une taxation d'office pour défaut de pièces
	 */
	TAXATION_OFFICE_DEFAUT_PIECES,

	/**
	 * La décision est une taxation d'office pour défaut de dossier
	 */
	TAXATION_OFFICE_DEFAUT_DOSSIER;

	/**
	 * @return si oui ou non nous avons affaire à une taxation d'office
	 */
	public boolean isTaxationOffice() {
		return this == TAXATION_OFFICE_DEFAUT_DOSSIER || this == TAXATION_OFFICE_DEFAUT_PIECES;
	}
}
