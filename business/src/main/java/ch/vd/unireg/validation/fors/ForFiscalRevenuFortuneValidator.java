package ch.vd.unireg.validation.fors;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public abstract class ForFiscalRevenuFortuneValidator<T extends ForFiscalRevenuFortune> extends ForFiscalAvecMotifsValidator<T> {

	public static final EnumSet<GenreImpot> DEFAULT_GENRE_IMPOTS = EnumSet.of(GenreImpot.REVENU_FORTUNE);

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			validateGenreImpot(vr, ff);

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

	@NotNull
	protected Set<GenreImpot> determineAllowedGenreImpots(@NotNull T forFiscal) {
		return DEFAULT_GENRE_IMPOTS;
	}

	private void validateGenreImpot(@NotNull ValidationResults vr, @NotNull T ff) {
		final Set<GenreImpot> allowed = determineAllowedGenreImpots(ff);
		if (!allowed.contains(ff.getGenreImpot())) {
			final String allowedString = (allowed.isEmpty() ? "(aucun)" : String.join(", ", allowed.stream().map(Enum::name).collect(Collectors.toList())));
			vr.addError(String.format("Le for %s avec genre d'impôt '%s' est invalide (autorisé(s) = %s).", getEntityDisplayString(ff), ff.getGenreImpot(), allowedString));
		}
	}

	protected abstract boolean isRattachementCoherent(MotifRattachement motif);
}
