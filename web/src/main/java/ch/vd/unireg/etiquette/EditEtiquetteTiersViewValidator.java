package ch.vd.unireg.etiquette;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.tiers.view.DateRangeViewValidator;

public class EditEtiquetteTiersViewValidator extends DateRangeViewValidator implements Validator {

	public EditEtiquetteTiersViewValidator() {
		super(false, true, true, true);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditEtiquetteTiersView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditEtiquetteTiersView view = (EditEtiquetteTiersView) target;
		super.validate(view, errors);

		if (StringUtils.isBlank(view.getCodeEtiquette())) {
			errors.rejectValue("codeEtiquette", "error.champ.obligatoire");
		}
	}
}
