package ch.vd.unireg.validation.registrefoncier;

import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
