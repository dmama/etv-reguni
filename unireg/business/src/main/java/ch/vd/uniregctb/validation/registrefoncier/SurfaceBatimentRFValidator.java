package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.registre.base.validation.ValidationResults;
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

	@Override
	public ValidationResults validate(SurfaceBatimentRF entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (entity.getSurface() == null && entity.getType() == null) {
				vr.addError(String.format("%s ne possède pas de type ni de surface renseigné", getEntityDisplayString(entity)));
			}
		}
		return vr;
	}
}
