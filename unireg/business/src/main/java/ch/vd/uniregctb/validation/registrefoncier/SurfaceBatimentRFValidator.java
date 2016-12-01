package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class SurfaceBatimentRFValidator extends DateRangeEntityValidator<SurfaceBatimentRF> {
	@Override
	protected Class<SurfaceBatimentRF> getValidatedClass() {
		return SurfaceBatimentRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La surface du bâtiment RF";
	}
}
