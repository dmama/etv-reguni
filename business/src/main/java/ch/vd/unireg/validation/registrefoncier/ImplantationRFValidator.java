package ch.vd.unireg.validation.registrefoncier;

import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
