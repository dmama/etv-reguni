package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalAutreElementImposableValidator extends ForFiscalRevenuFortuneValidator<ForFiscalAutreElementImposable> {

	@Override
	protected Class<ForFiscalAutreElementImposable> getValidatedClass() {
		return ForFiscalAutreElementImposable.class;
	}

	@Override
	public ValidationResults validate(ForFiscalAutreElementImposable ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {
			final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
			if (typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'autre élément imposable' est limité à COMMUNE_OU_FRACTION_VD");
			}
		}
		return vr;
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.ACTIVITE_LUCRATIVE_CAS == motif
				|| MotifRattachement.ADMINISTRATEUR == motif
				|| MotifRattachement.CREANCIER_HYPOTHECAIRE == motif
				|| MotifRattachement.PRESTATION_PREVOYANCE == motif
				|| MotifRattachement.LOI_TRAVAIL_AU_NOIR == motif;
	}
}
