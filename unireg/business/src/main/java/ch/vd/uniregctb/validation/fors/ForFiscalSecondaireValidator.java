package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalSecondaireValidator extends ForFiscalRevenuFortuneValidator<ForFiscalSecondaire> {

	@Override
	protected Class<ForFiscalSecondaire> getValidatedClass() {
		return ForFiscalSecondaire.class;
	}

	@Override
	public ValidationResults validate(ForFiscalSecondaire ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {
			if (ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal secondaire est limité à COMMUNE_OU_FRACTION_VD");
			}
		}
		return vr;
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.ACTIVITE_INDEPENDANTE == motif
				|| MotifRattachement.IMMEUBLE_PRIVE == motif
				|| MotifRattachement.SEJOUR_SAISONNIER == motif
				|| MotifRattachement.DIRIGEANT_SOCIETE == motif;
	}
}
