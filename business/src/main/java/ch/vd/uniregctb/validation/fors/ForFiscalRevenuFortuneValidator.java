package ch.vd.uniregctb.validation.fors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class ForFiscalRevenuFortuneValidator<T extends ForFiscalRevenuFortune> extends ForFiscalAvecMotifsValidator<T> {

	@Override
	public ValidationResults validate(T ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			if (!isGenreImpotCoherent(ff)) {
				vr.addError(String.format("Le for %s avec genre d'impôt '%s' est invalide.", getEntityDisplayString(ff), ff.getGenreImpot()));
			}

			final MotifRattachement motifRattachement = ff.getMotifRattachement();
			if (!isRattachementCoherent(motifRattachement)){
				vr.addError(String.format("Le for %s avec motif de rattachement = %s est invalide", getEntityDisplayString(ff), motifRattachement));
			}

			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (ff.getMotifOuverture() == null) {
					vr.addError(String.format("Le motif d'ouverture est obligatoire sur le for fiscal [%s] car il est rattaché à une commune vaudoise.", getEntityDisplayString(ff)));
				}
				if (ff.getMotifFermeture() == null && ff.getDateFin() != null) {
					vr.addError(String.format("Le motif de fermeture est obligatoire sur le for fiscal [%s] car il est rattaché à une commune vaudoise.", getEntityDisplayString(ff)));
				}
			}
		}
		return vr;
	}

	protected boolean isGenreImpotCoherent(@NotNull T ff) {
		return ff.getGenreImpot() == GenreImpot.REVENU_FORTUNE;
	}

	protected abstract boolean isRattachementCoherent(MotifRattachement motif);
}
