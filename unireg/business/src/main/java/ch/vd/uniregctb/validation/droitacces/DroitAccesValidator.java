package ch.vd.uniregctb.validation.droitacces;

import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

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
