package ch.vd.unireg.validation.fors;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class ForFiscalSecondaireValidator extends ForFiscalRevenuFortuneValidator<ForFiscalSecondaire> {

	private static final Set<GenreImpot> GENRES_IMPOT_AUTORISES = EnumSet.of(GenreImpot.BENEFICE_CAPITAL, GenreImpot.REVENU_FORTUNE);

	@Override
	protected Class<ForFiscalSecondaire> getValidatedClass() {
		return ForFiscalSecondaire.class;
	}

	@NotNull
	@Override
	public ValidationResults validate(@NotNull ForFiscalSecondaire ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {
			if (ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal secondaire est limité à COMMUNE_OU_FRACTION_VD");
			}
		}
		return vr;
	}

	@NotNull
	@Override
	protected Set<GenreImpot> determineAllowedGenreImpots(@NotNull ForFiscalSecondaire forFiscal) {
		return GENRES_IMPOT_AUTORISES;
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
