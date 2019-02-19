package ch.vd.unireg.validation.registrefoncier;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class SurfaceTotaleRFValidator extends DateRangeEntityValidator<SurfaceTotaleRF> {
	@Override
	protected Class<SurfaceTotaleRF> getValidatedClass() {
		return SurfaceTotaleRF.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull SurfaceTotaleRF entity) {
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
