package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockSuppressionNationalite extends MockEvenementCivil implements SuppressionNationalite {

	private boolean nationaliteSuisse;

	public MockSuppressionNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean nationaliteSuisse) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.SUP_NATIONALITE_SUISSE : TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce);
		this.nationaliteSuisse = nationaliteSuisse;
	}

	/**
	 * @return the nationaliteSuisse
	 */
	public boolean isNationaliteSuisse() {
		return nationaliteSuisse;
	}
}
