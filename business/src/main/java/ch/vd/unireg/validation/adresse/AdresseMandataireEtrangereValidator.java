package ch.vd.unireg.validation.adresse;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseMandataireEtrangere;

public class AdresseMandataireEtrangereValidator extends AdresseMandataireValidator<AdresseMandataireEtrangere> {

	@Override
	protected Class<AdresseMandataireEtrangere> getValidatedClass() {
		return AdresseMandataireEtrangere.class;
	}

	@NotNull
	@Override
	public ValidationResults validate(@NotNull AdresseMandataireEtrangere adr) {
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
