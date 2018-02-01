package ch.vd.unireg.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdressePM;

public class AdressePMValidator extends AdresseTiersValidator<AdressePM> {

	@Override
	protected Class<AdressePM> getValidatedClass() {
		return AdressePM.class;
	}

	@Override
	public ValidationResults validate(AdressePM adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			if (adr.getType() == null) {
				vr.addError(String.format("Le type d'adresse doit être renseigné sur une adresse PM [%s]", getEntityDisplayString(adr)));
			}
		}
		return vr;
	}
}
