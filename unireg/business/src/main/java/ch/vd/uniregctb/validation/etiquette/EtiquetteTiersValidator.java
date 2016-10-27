package ch.vd.uniregctb.validation.etiquette;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

/**
 * Validateur d'une étiquette liée temporellement à un tiers
 */
public class EtiquetteTiersValidator extends DateRangeEntityValidator<EtiquetteTiers> {

	@Override
	protected Class<EtiquetteTiers> getValidatedClass() {
		return EtiquetteTiers.class;
	}

	@Override
	public ValidationResults validate(EtiquetteTiers entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (entity.getEtiquette() == null) {
				vr.addError("etiquette", "Le lien d'étiquetage doit être associé à une étiquette.");
			}
			else {
				vr.merge(getValidationService().validate(entity.getEtiquette()));
			}
		}
		return vr;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'étiquette";
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
