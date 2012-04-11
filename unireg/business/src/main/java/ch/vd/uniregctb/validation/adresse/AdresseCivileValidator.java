package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;

public class AdresseCivileValidator extends AdresseTiersValidator<AdresseCivile> {

	@Override
	protected Class<AdresseCivile> getValidatedClass() {
		return AdresseCivile.class;
	}

	@Override
	public ValidationResults validate(AdresseCivile adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			if (adr.getType() == null) {
				vr.addError(String.format("Le type d'adresse doit être renseigné sur une adresse civile [%s]", adr));
			}
		}
		return vr;
	}
}
