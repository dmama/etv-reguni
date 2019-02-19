package ch.vd.unireg.validation.adresse;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseCivile;

public class AdresseCivileValidator extends AdresseTiersValidator<AdresseCivile> {

	@Override
	protected Class<AdresseCivile> getValidatedClass() {
		return AdresseCivile.class;
	}

	@NotNull
	@Override
	public ValidationResults validate(AdresseCivile adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			if (adr.getType() == null) {
				vr.addError(String.format("Le type d'adresse doit être renseigné sur une adresse civile [%s]", getEntityDisplayString(adr)));
			}
		}
		return vr;
	}
}
