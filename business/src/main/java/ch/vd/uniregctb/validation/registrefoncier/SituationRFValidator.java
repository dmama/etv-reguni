package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class SituationRFValidator extends DateRangeEntityValidator<SituationRF> {
	@Override
	protected Class<SituationRF> getValidatedClass() {
		return SituationRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La situation RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
