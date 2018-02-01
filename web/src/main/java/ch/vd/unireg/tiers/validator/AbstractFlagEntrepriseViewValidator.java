package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.tiers.view.DateRangeViewValidator;

public abstract class AbstractFlagEntrepriseViewValidator implements Validator {

	private static final DateRangeViewValidator RANGE_VALIDATOR = new DateRangeViewValidator(false, true, true, true);

	protected void validateRange(DateRange range, Errors errors) {
		RANGE_VALIDATOR.validate(range, errors);
	}
}
