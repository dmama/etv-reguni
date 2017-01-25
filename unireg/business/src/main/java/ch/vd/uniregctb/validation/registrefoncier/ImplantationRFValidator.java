package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class ImplantationRFValidator extends DateRangeEntityValidator<ImplantationRF> {
	@Override
	protected Class<ImplantationRF> getValidatedClass() {
		return ImplantationRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'implantation RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
