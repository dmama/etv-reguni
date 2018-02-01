package ch.vd.unireg.tiers.view;

import org.jetbrains.annotations.Nullable;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class DateRangeViewValidator {

	/**
	 * [SIFISC-19932] on autorise une saisie de dates dans le futur jusqu'à 20 ans, pas plus...
	 */
	private static final int DEFAUT_MAX_NOMBRE_ANNEES_DANS_FUTUR_POUR_DATES = 20;

	private final boolean allowedNullDebut;
	private final boolean allowedNullFin;
	private final boolean allowedFutureDebut;
	private final boolean allowedFutureFin;
	private final Integer maxAnneesDansFutur;

	public DateRangeViewValidator(boolean allowedNullDebut, boolean allowedNullFin, boolean allowedFutureDebut, boolean allowedFutureFin) {
		this(allowedNullDebut, allowedNullFin, allowedFutureDebut, allowedFutureFin, DEFAUT_MAX_NOMBRE_ANNEES_DANS_FUTUR_POUR_DATES);
	}

	public DateRangeViewValidator(boolean allowedNullDebut, boolean allowedNullFin, boolean allowedFutureDebut, boolean allowedFutureFin, @Nullable Integer maxAnneesDansFutur) {
		this.allowedNullDebut = allowedNullDebut;
		this.allowedNullFin = allowedNullFin;
		this.allowedFutureDebut = allowedFutureDebut;
		this.allowedFutureFin = allowedFutureFin;
		this.maxAnneesDansFutur = maxAnneesDansFutur;
	}

	public void validate(DateRange range, Errors errors) {

		final RegDate now = RegDate.get();
		final RegDate maxFutur = maxAnneesDansFutur != null ? now.addYears(maxAnneesDansFutur) : null;
		final RegDate dateDebut = range.getDateDebut();
		final RegDate dateFin = range.getDateFin();

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (dateDebut == null && !allowedNullDebut) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
			else if (dateDebut != null && now.isBefore(dateDebut)) {
				if (!allowedFutureDebut) {
					errors.rejectValue("dateDebut", "error.date.debut.future");
				}
				else if (maxFutur != null && RegDateHelper.isBefore(maxFutur, dateDebut, NullDateBehavior.LATEST)) {
					errors.rejectValue("dateDebut", "error.date.plus.de.x.annees.dans.futur", new Object[]{Integer.toString(maxAnneesDansFutur)}, null);
				}
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (dateFin == null && !allowedNullFin) {
				errors.rejectValue("dateFin", "error.date.fin.vide");
			}
			else if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
			else if (dateFin != null && now.isBefore(dateFin)) {
				if (!allowedFutureFin) {
					errors.rejectValue("dateFin", "error.date.fin.dans.futur");
				}
				else if (maxFutur != null && RegDateHelper.isBefore(maxFutur, dateFin, NullDateBehavior.LATEST)) {
					errors.rejectValue("dateFin", "error.date.plus.de.x.annees.dans.futur", new Object[]{Integer.toString(maxAnneesDansFutur)}, null);
				}
			}
		}
	}
}
