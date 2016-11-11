package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class SurfaceAuSolRFValidator extends DateRangeEntityValidator<SurfaceAuSolRF> {
	@Override
	protected Class<SurfaceAuSolRF> getValidatedClass() {
		return SurfaceAuSolRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La surface au sol RF";
	}
}
