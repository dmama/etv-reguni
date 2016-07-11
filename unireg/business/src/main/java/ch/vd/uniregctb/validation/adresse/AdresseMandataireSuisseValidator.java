package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;

public class AdresseMandataireSuisseValidator extends AdresseMandataireValidator<AdresseMandataireSuisse> {

	@Override
	protected Class<AdresseMandataireSuisse> getValidatedClass() {
		return AdresseMandataireSuisse.class;
	}

	@Override
	public ValidationResults validate(AdresseMandataireSuisse adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			final Integer numeroOrdrePoste = adr.getNumeroOrdrePoste();
			if (numeroOrdrePoste == null) {
				vr.addError(String.format("Le numéro d'ordre poste doit être renseigné sur une adresse suisse [%s]", getEntityDisplayString(adr)));
			}
		}
		return vr;
	}
}
