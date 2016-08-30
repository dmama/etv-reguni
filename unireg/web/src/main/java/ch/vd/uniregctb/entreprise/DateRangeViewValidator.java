package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public abstract class DateRangeViewValidator<T extends DateRange> implements Validator {

	private final Class<T> supportedClass;

	public DateRangeViewValidator(Class<T> supportedClass) {
		this.supportedClass = supportedClass;
	}

	@Override
	public final boolean supports(Class<?> clazz) {
		return supportedClass.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final DateRange view = (DateRange) target;
		final RegDate today = RegDate.get();
		final RegDate dateDebut = view.getDateDebut();
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors(getDateDebutName())) {
			if (dateDebut == null) {
				errors.rejectValue(getDateDebutName(), getDateDebutVideMessage());
			}
			else if (today.isBefore(dateDebut)) {
				errors.rejectValue(getDateDebutName(), getDateDebutFutureMessage());
			}
		}

		if (dateDebut != null && view.getDateFin() != null) {
			if (view.getDateFin().isBefore(dateDebut)) {
				errors.rejectValue(getDateFinName(), getDateFinAvantDebutMessage());
			}
			else if (today.isBefore(view.getDateFin())) {
				errors.rejectValue(getDateFinName(), getDateFinFutureMessage());
			}
		}
	}

	protected String getDateDebutName() {
		return "dateDebut";
	}

	protected String getDateFinName() {
		return "dateFin";
	}

	protected String getDateDebutVideMessage() {
		return "error.date.debut.vide";
	}

	protected String getDateDebutFutureMessage() {
		return "error.date.debut.future";
	}

	protected String getDateFinAvantDebutMessage() {
		return "error.date.fin.avant.debut";
	}

	protected String getDateFinFutureMessage() {
		return "error.date.fin.dans.futur";
	}
}
