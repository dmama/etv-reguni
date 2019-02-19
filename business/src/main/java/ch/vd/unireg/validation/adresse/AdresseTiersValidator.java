package ch.vd.unireg.validation.adresse;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public abstract class AdresseTiersValidator<T extends AdresseTiers> extends DateRangeEntityValidator<T> {

	@Override
	@NotNull
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
