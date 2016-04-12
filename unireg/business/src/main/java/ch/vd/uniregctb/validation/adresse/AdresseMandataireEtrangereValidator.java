package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseMandataireEtrangere;

public class AdresseMandataireEtrangereValidator extends AdresseMandataireValidator<AdresseMandataireEtrangere> {

	@Override
	protected Class<AdresseMandataireEtrangere> getValidatedClass() {
		return AdresseMandataireEtrangere.class;
	}

	@Override
	public ValidationResults validate(AdresseMandataireEtrangere adr) {
		final ValidationResults vr = super.validate(adr);
		if (!adr.isAnnule()) {
			final Integer numeroOfsPays = adr.getNumeroOfsPays();
			if (numeroOfsPays == null || numeroOfsPays == 0) {
				vr.addError(String.format("Le numéro Ofs du pays doit être renseigné sur une adresse étrangère [%s]", getEntityDisplayString(adr)));
			}
		}
		return vr;
	}
}
