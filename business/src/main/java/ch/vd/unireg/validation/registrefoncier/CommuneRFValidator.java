package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class CommuneRFValidator extends DateRangeEntityValidator<CommuneRF> {
	@Override
	protected Class<CommuneRF> getValidatedClass() {
		return CommuneRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La commune RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
