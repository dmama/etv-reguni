package ch.vd.unireg.validation.fors;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class ForFiscalAutreElementImposableValidator extends ForFiscalRevenuFortuneValidator<ForFiscalAutreElementImposable> {

	private static final Set<MotifRattachement> ALLOWED = EnumSet.of(MotifRattachement.ACTIVITE_LUCRATIVE_CAS, MotifRattachement.ADMINISTRATEUR, MotifRattachement.CREANCIER_HYPOTHECAIRE,
	                                                                 MotifRattachement.PRESTATION_PREVOYANCE, MotifRattachement.LOI_TRAVAIL_AU_NOIR, MotifRattachement.PARTICIPATIONS_HORS_SUISSE,
	                                                                 MotifRattachement.EFFEUILLEUSES);

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
		return ALLOWED.contains(motif);
	}
}
