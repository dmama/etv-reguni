package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalAutreImpotValidator extends ForFiscalValidator<ForFiscalAutreImpot> {

	@Override
	protected Class<ForFiscalAutreImpot> getValidatedClass() {
		return ForFiscalAutreImpot.class;
	}

	@Override
	public ValidationResults validate(ForFiscalAutreImpot ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {
			if (ff.getGenreImpot() == GenreImpot.REVENU_FORTUNE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de REVENU_FORTUNE.");
			}

			if (ff.getGenreImpot() == GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de DEBITEUR_PRESTATION_IMPOSABLE.");
			}

			if (ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'autre impôt' est limité à COMMUNE_OU_FRACTION_VD");
			}
		}
		return vr;
	}
}
