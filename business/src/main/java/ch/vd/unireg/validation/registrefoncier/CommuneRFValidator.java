package ch.vd.unireg.validation.registrefoncier;

import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
