package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class DroitRFValidator extends DateRangeEntityValidator<DroitRF> {
	@Override
	protected Class<DroitRF> getValidatedClass() {
		return DroitRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le droit RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
