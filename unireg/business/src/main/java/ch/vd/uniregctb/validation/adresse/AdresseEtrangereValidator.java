package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEtrangere;

public class AdresseEtrangereValidator extends AdresseSupplementaireValidator<AdresseEtrangere> {

	@Override
	protected Class<AdresseEtrangere> getValidatedClass() {
		return AdresseEtrangere.class;
	}

	@Override
	public ValidationResults validate(AdresseEtrangere adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			final Integer numeroOfsPays = adr.getNumeroOfsPays();
			if (numeroOfsPays == null || numeroOfsPays == 0) {
				vr.addError(String.format("Le numéro Ofs du pays doit être renseigné sur une adresse étrangère [%s]", adr));
			}
		}
		return vr;
	}
}
