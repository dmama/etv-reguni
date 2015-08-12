package ch.vd.uniregctb.validation.fors;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalSecondaireValidator extends ForFiscalRevenuFortuneValidator<ForFiscalSecondaire> {

	private static final Set<GenreImpot> GENRES_IMPOT_AUTORISES = EnumSet.of(GenreImpot.BENEFICE_CAPITAL, GenreImpot.REVENU_FORTUNE);

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
	protected boolean isGenreImpotCoherent(GenreImpot genreImpot) {
		return GENRES_IMPOT_AUTORISES.contains(genreImpot);
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.ACTIVITE_INDEPENDANTE == motif
				|| MotifRattachement.IMMEUBLE_PRIVE == motif
				|| MotifRattachement.SEJOUR_SAISONNIER == motif
				|| MotifRattachement.ETABLISSEMENT_STABLE == motif
				|| MotifRattachement.DIRIGEANT_SOCIETE == motif;
	}
}
