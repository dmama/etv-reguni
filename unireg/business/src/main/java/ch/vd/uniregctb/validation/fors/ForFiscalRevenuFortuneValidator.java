package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class ForFiscalRevenuFortuneValidator<T extends ForFiscalRevenuFortune> extends ForFiscalValidator<T> {

	@Override
	public ValidationResults validate(T ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			if (ff.getGenreImpot() != GenreImpot.REVENU_FORTUNE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'revenu-fortune' doit être REVENU_FORTUNE.");
			}

			final MotifRattachement motifRattachement = ff.getMotifRattachement();
			if (!isRattachementCoherent(motifRattachement)){
				vr.addError(String.format("Le for %s avec motif de rattachement = %s est invalide", ff, motifRattachement));
			}

			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (ff.getMotifOuverture() == null) {
					vr.addError(String.format("Le motif d'ouverture est obligatoire sur le for fiscal [%s] car il est rattaché à une commune vaudoise.", ff));
				}
				if (ff.getMotifFermeture() == null && ff.getDateFin() != null) {
					vr.addError(String.format("Le motif de fermeture est obligatoire sur le for fiscal [%s] car il est rattaché à une commune vaudoise.", ff));
				}
			}
		}
		return vr;
	}

	protected abstract boolean isRattachementCoherent(MotifRattachement motif);
}
