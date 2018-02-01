package ch.vd.unireg.mandataire;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.DateRangeViewValidator;
import ch.vd.unireg.type.TypeMandat;

public class EditMandatViewValidator implements Validator {

	private static final DateRangeViewValidator RANGE_VALIDATOR = new DateRangeViewValidator(false, true, true, true);

	private final IbanValidator ibanValidator;

	public EditMandatViewValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditMandatView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditMandatView view = (EditMandatView) target;

		// validation des dates
		RANGE_VALIDATOR.validate(view, errors);

		// mandat tiers -> vérification de l'IBAN
		if (view.getTypeMandat() == TypeMandat.TIERS) {
			if (StringUtils.isBlank(view.getIban())) {
				errors.rejectValue("iban", "error.iban.mandat.tiers.vide");
			}
			else {
				final String erreurIban = ibanValidator.getIbanValidationError(view.getIban());
				if (StringUtils.isNotBlank(erreurIban)) {
					errors.rejectValue("iban", "error.iban.detail", new Object[]{erreurIban}, null);
				}
			}
		}
	}
}
