package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForDebiteurPrestationImposableValidator extends ForFiscalValidator<ForDebiteurPrestationImposable> {

	@Override
	protected Class<ForDebiteurPrestationImposable> getValidatedClass() {
		return ForDebiteurPrestationImposable.class;
	}

	@Override
	public ValidationResults validate(ForDebiteurPrestationImposable ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			if (ff.getGenreImpot() != GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'débiteur prestation imposable' doit être DEBITEUR_PRESTATION_IMPOSABLE.");
			}

			final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
			if (typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_HC) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'débiteur prestation imposable' est limité à COMMUNE_OU_FRACTION_VD ou COMMUNE_HC");
			}
		}
		return vr;
	}
}
