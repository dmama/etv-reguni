package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalPrincipalValidator extends ForFiscalRevenuFortuneValidator<ForFiscalPrincipal> {

	@Override
	protected Class<ForFiscalPrincipal> getValidatedClass() {
		return ForFiscalPrincipal.class;
	}

	@Override
	public ValidationResults validate(ForFiscalPrincipal ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			final ModeImposition modeImposition = ff.getModeImposition();
			if (modeImposition == null) {
				vr.addError("Le mode d'imposition est obligatoire sur un for fiscal principal.");
			}
			else if (ff.getMotifRattachement() == MotifRattachement.DOMICILE && ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (modeImposition != ModeImposition.ORDINAIRE && modeImposition != ModeImposition.SOURCE) {
					vr.addError("Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, " +
							"les modes d'imposition possibles sont \"ordinaire\" ou \"source\".");
				}
			}

			// [UNIREG-911]
			if (ff.getMotifOuverture() == MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE && ff.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE) {
				vr.addError("Le motif de début d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger");
			}

			if (ff.getMotifFermeture() == MotifFor.FIN_ACTIVITE_DIPLOMATIQUE && ff.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE) {
				vr.addError("Le motif de fin d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger");
			}
		}
		return vr;
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.DOMICILE == motif
				|| MotifRattachement.DIPLOMATE_SUISSE == motif
				|| MotifRattachement.DIPLOMATE_ETRANGER == motif;
	}
}
