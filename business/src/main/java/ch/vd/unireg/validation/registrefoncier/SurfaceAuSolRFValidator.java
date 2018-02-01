package ch.vd.unireg.validation.registrefoncier;

import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class SurfaceAuSolRFValidator extends DateRangeEntityValidator<SurfaceAuSolRF> {
	@Override
	protected Class<SurfaceAuSolRF> getValidatedClass() {
		return SurfaceAuSolRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La surface au sol RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
