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
		if (view.getDateDebut() == null) {
			errors.rejectValue(getDateDebutName(), getDateDebutVideMessage());
		}
		else {
			final RegDate today = RegDate.get();
			if (today.isBefore(view.getDateDebut())) {
				errors.rejectValue(getDateDebutName(), getDateDebutFutureMessage());
			}
			if (view.getDateFin() != null) {
				if (view.getDateFin().isBefore(view.getDateDebut())) {
					errors.rejectValue(getDateFinName(), getDateFinAvantDebutMessage());
				}
				else if (today.isBefore(view.getDateFin())) {
					errors.rejectValue(getDateFinName(), getDateFinFutureMessage());
				}
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
