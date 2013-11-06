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

	private static final Set<MotifFor> ALLOWED_OPENING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.DEBUT_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION, MotifFor.DEMENAGEMENT_SIEGE);
	private static final Set<MotifFor> ALLOWED_CLOSING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FIN_PRESTATION_IS, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.FUSION_COMMUNES, MotifFor.ANNULATION, MotifFor.DEMENAGEMENT_SIEGE);

	private static final Set<MotifFor> OPENING_MONTH_BEGINNING = EnumSet.of(MotifFor.DEBUT_PRESTATION_IS, MotifFor.DEMENAGEMENT_SIEGE);
	private static final Set<MotifFor> CLOSING_MONTH_END = EnumSet.of(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.DEMENAGEMENT_SIEGE);
	private static final Set<MotifFor> CLOSING_YEAR_END = EnumSet.of(MotifFor.FIN_PRESTATION_IS);

	@Override
	protected Class<ForDebiteurPrestationImposable> getValidatedClass() {
		return ForDebiteurPrestationImposable.class;
	}

	/**
	 * [SIFISC-10141] les fors DPI peuvent avoir des dates de fermeture dans le futur
	 * @return <code>true</code>
	 */
	@Override
	protected boolean isDateFermetureFutureAllowed() {
		return true;
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
				vr.addError("Le motif d'ouverture '" + ff.getMotifOuverture().getDescription(true) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			if (ff.getMotifFermeture() != null && !ALLOWED_CLOSING_CAUSES.contains(ff.getMotifFermeture())) {
				vr.addError("Le motif de fermeture '" + ff.getMotifFermeture().getDescription(false) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}

			// [SIFISC-8712] le motif d'ouverture est obligatoire
			if (ff.getMotifOuverture() == null) {
				vr.addError("Le motif d'ouverture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			// [SIFISC-8712] conformité du motif d'ouverture avec la date correspondante
			else if (ff.getDateDebut() != null && OPENING_MONTH_BEGINNING.contains(ff.getMotifOuverture()) && ff.getDateDebut().day() != 1) {
				vr.addError("Les fors ouverts avec le motif '" + ff.getMotifOuverture().getDescription(true) + "' doivent commencer un premier jour du mois.");
			}

			if (ff.getDateFin() != null && ff.getDateFin().isAfter(getFutureBeginDate())) {
				// [SIFISC-10141] si la date de fin est dans le futur :
				//  - seul le 31.12 courant est autorisé
				//  - seul le motif FIN_PRESTATION_IS est autorisé
				final RegDate allowed = RegDate.get(getFutureBeginDate().year(), 12, 31);
				if (ff.getDateFin() != allowed) {
					vr.addError("Une date de fin dans le futur ne peut être que le 31.12 de l'année courante.");
				}
				if (ff.getMotifFermeture() != MotifFor.FIN_PRESTATION_IS) {
					vr.addError("Seul le motif '" + MotifFor.FIN_PRESTATION_IS.getDescription(false) + "' est autorisé pour une fermeture dans le futur.");
				}
			}
			else if (ff.getDateFin() != null) {
				// [SIFISC-8712] le motif de fermeture est obligatoire si le for est fermé
				if (ff.getMotifFermeture() == null) {
					vr.addError("Le motif de fermeture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable' fermés.");
				}
				// [SIFISC-8712] conformité du motif de fermeture avec la date correspondante
				else if (CLOSING_YEAR_END.contains(ff.getMotifFermeture()) && ff.getDateFin() != RegDate.get(ff.getDateFin().year(), 12, 31)) {
					vr.addError("Les fors fermés avec le motif '" + ff.getMotifFermeture().getDescription(false) + "' doivent être fermés à une fin d'année.");
				}
				else if (CLOSING_MONTH_END.contains(ff.getMotifFermeture()) && ff.getDateFin() != ff.getDateFin().getLastDayOfTheMonth()) {
					vr.addError("Les fors fermés avec le motif '" + ff.getMotifFermeture().getDescription(false) + "' doivent être fermés à une fin de mois.");
				}
			}
		}
		return vr;
	}
}
