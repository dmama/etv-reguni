package ch.vd.unireg.validation.droitacces;

import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class DroitAccesValidator extends DateRangeEntityValidator<DroitAcces> {

	@Override
	protected Class<DroitAcces> getValidatedClass() {
		return DroitAcces.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le droit d'accès";
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
