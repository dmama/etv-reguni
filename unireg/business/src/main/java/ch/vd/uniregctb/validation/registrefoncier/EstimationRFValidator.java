package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class EstimationRFValidator extends DateRangeEntityValidator<EstimationRF> {
	@Override
	protected Class<EstimationRF> getValidatedClass() {
		return EstimationRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'estimation fiscale RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
