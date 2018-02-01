package ch.vd.unireg.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseAutreTiers;

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
				vr.addError(String.format("Le type d'adresse doit être renseigné sur une adresse 'autre tiers' [%s]", getEntityDisplayString(adr)));
			}
			if (adr.getAutreTiersId() == null) {
				vr.addError(String.format("Le tiers cible doit être renseigné sur une adresse 'autre tiers' [%s]", getEntityDisplayString(adr)));
			}
			else if (adr.getAutreTiersId().equals(adr.getTiers().getId())) { // [UNIREG-3152]
				vr.addError(String.format("Le tiers cible doit être différent du tiers courant sur une adresse 'autre tiers' [%s]", getEntityDisplayString(adr)));
			}
		}
		return vr;
	}
}
