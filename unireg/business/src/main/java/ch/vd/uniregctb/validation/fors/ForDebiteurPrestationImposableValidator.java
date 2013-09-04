package ch.vd.uniregctb.validation.fors;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForDebiteurPrestationImposableValidator extends ForFiscalAvecMotifsValidator<ForDebiteurPrestationImposable> {

	private static final Set<MotifFor> ALLOWED_OPENING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.DEBUT_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION);
	private static final Set<MotifFor> ALLOWED_CLOSING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FIN_PRESTATION_IS, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.FUSION_COMMUNES, MotifFor.ANNULATION);

	@Override
	protected Class<ForDebiteurPrestationImposable> getValidatedClass() {
		return ForDebiteurPrestationImposable.class;
	}

	@Override
	public ValidationResults validate(ForDebiteurPrestationImposable ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			if (ff.getGenreImpot() != GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'débiteur prestation imposable' doit être DEBITEUR_PRESTATION_IMPOSABLE.");
			}

			final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
			if (typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_HC) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'débiteur prestation imposable' est limité à COMMUNE_OU_FRACTION_VD ou COMMUNE_HC");
			}

			// [SIFISC-8712] ensemble des valeurs autorisées pour les motifs d'ouverture/de fermeture
			if (ff.getMotifOuverture() != null && !ALLOWED_OPENING_CAUSES.contains(ff.getMotifOuverture())) {
				vr.addError("Le motif d'ouverture " + ff.getMotifOuverture() + " n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			if (ff.getMotifFermeture() != null && !ALLOWED_CLOSING_CAUSES.contains(ff.getMotifFermeture())) {
				vr.addError("Le motif de fermeture " + ff.getMotifFermeture() + " n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}

			// [SIFISC-8712] les motifs d'ouverture et de fermeture sont obligatoires
			if (ff.getMotifOuverture() == null) {
				vr.addError("Le motif d'ouverture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			if (ff.getDateFin() != null && ff.getMotifFermeture() == null) {
				vr.addError("Le motif de fermeture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable' fermés.");
			}

			// [SIFISC-8712] conformité des motifs d'ouverture et de fermeture des fors avec les dates correspondantes
			if (ff.getDateDebut() != null && ff.getMotifOuverture() == MotifFor.DEBUT_PRESTATION_IS && ff.getDateDebut().day() != 1) {
				vr.addError("Les fors ouverts avec le motif '" + MotifFor.DEBUT_PRESTATION_IS.getDescription(true) + "' doivent commencer un premier jour du mois.");
			}
			if (ff.getDateFin() != null) {
				if (ff.getMotifFermeture() == MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE && ff.getDateFin() != ff.getDateFin().getLastDayOfTheMonth()) {
					vr.addError("Les fors fermés avec le motif '" + MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE.getDescription(false) + "' doivent être fermés à une fin de mois.");
				}
				if (ff.getMotifFermeture() == MotifFor.FIN_PRESTATION_IS && ff.getDateFin() != RegDate.get(ff.getDateFin().year(), 12, 31)) {
					vr.addError("Les fors fermés avec le motif '" + MotifFor.FIN_PRESTATION_IS.getDescription(false) + "' doivent être fermés à une fin d'année.");
				}
			}
		}
		return vr;
	}
}
