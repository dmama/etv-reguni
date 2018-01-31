package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class DescriptionBatimentRFValidator extends DateRangeEntityValidator<DescriptionBatimentRF> {
	@Override
	protected Class<DescriptionBatimentRF> getValidatedClass() {
		return DescriptionBatimentRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La description du bâtiment RF";
	}

	@Override
	public ValidationResults validate(DescriptionBatimentRF entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (entity.getSurface() == null && entity.getType() == null) {
				vr.addError(String.format("%s ne possède pas de type ni de surface renseigné", getEntityDisplayString(entity)));
			}
		}
		return vr;
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
