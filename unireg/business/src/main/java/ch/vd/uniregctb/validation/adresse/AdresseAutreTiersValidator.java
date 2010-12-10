package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;

public class AdresseAutreTiersValidator extends AdresseTiersValidator<AdresseAutreTiers> {

	@Override
	protected Class<AdresseAutreTiers> getValidatedClass() {
		return AdresseAutreTiers.class;
	}

	@Override
	public ValidationResults validate(AdresseAutreTiers adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			if (adr.getType() == null) {
				vr.addError(String.format("Le type d'adresse doit être renseigné sur une adresse 'autre tiers' [%s]", adr));
			}
			if (adr.getAutreTiersId() == null) {
				vr.addError(String.format("Le tiers cible doit être renseigné sur une adresse 'autre tiers' [%s]", adr));
			}
		}
		return vr;
	}
}
