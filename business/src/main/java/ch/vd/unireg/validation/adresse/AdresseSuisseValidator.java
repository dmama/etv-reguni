package ch.vd.unireg.validation.adresse;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseSuisse;

public class AdresseSuisseValidator extends AdresseSupplementaireValidator<AdresseSuisse> {

	@Override
	protected Class<AdresseSuisse> getValidatedClass() {
		return AdresseSuisse.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(AdresseSuisse adr) {
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
