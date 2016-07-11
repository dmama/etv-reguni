package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public abstract class AdresseTiersValidator<T extends AdresseTiers> extends DateRangeEntityValidator<T> {

	@Override
	public ValidationResults validate(T adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			// L'usage doit être renseigné
			if (adr.getUsage() == null) {
				vr.addError(String.format("%s %s possède un usage nul", getEntityCategoryName(), getEntityDisplayString(adr)));
			}
		}
		return vr;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'adresse";
	}
}
