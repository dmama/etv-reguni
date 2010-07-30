package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockSuppressionNationalite extends MockEvenementCivil implements SuppressionNationalite {

	private boolean nationaliteSuisse;

	/**
	 * @return the nationaliteSuisse
	 */
	public boolean isNationaliteSuisse() {
		return nationaliteSuisse;
	}

	/**
	 * @param nationaliteSuisse the nationaliteSuisse to set
	 */
	public void setNationaliteSuisse(boolean nationaliteSuisse) {
		this.nationaliteSuisse = nationaliteSuisse;
	}
}
