package ch.vd.unireg.validation.registrefoncier;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class SurfaceTotaleRFValidator extends DateRangeEntityValidator<SurfaceTotaleRF> {
	@Override
	protected Class<SurfaceTotaleRF> getValidatedClass() {
		return SurfaceTotaleRF.class;
	}

	@Override
	public ValidationResults validate(SurfaceTotaleRF entity) {
		return super.validate(entity);
	}

	@Override
	protected String getEntityCategoryName() {
		return "La surface totale RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
