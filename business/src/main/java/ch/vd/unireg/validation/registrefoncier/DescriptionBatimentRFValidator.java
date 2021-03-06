package ch.vd.unireg.validation.registrefoncier;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.DescriptionBatimentRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
	@NotNull
	public ValidationResults validate(@NotNull DescriptionBatimentRF entity) {
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
