package ch.vd.unireg.validation.registrefoncier;

import ch.vd.unireg.registrefoncier.ServitudeRF;

@SuppressWarnings("Duplicates")
public class ServitudeRFValidator extends DroitRFValidator<ServitudeRF> {
	@Override
	protected Class<ServitudeRF> getValidatedClass() {
		return ServitudeRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La servitude RF";
	}
}
