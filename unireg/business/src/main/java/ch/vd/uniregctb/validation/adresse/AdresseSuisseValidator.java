package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseSuisse;

public class AdresseSuisseValidator extends AdresseSupplementaireValidator<AdresseSuisse> {

	@Override
	protected Class<AdresseSuisse> getValidatedClass() {
		return AdresseSuisse.class;
	}

	@Override
	public ValidationResults validate(AdresseSuisse adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			final Integer numeroRue = adr.getNumeroRue();
			final Integer numeroOrdrePoste = adr.getNumeroOrdrePoste();
			if ((numeroRue == null || numeroRue == 0) && (numeroOrdrePoste == null || numeroOrdrePoste == 0)) {
				vr.addError(String.format("Le numéro de rue ou le numéro d'ordre poste doit être renseigné sur une adresse suisse [%s]", adr));
			}
		}
		return vr;
	}
}
