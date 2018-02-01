package ch.vd.unireg.etiquette;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.tiers.view.DateRangeViewValidator;

public class AddEtiquetteTiersViewValidator extends DateRangeViewValidator implements Validator {

	public AddEtiquetteTiersViewValidator() {
		super(false, true, true, true);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddEtiquetteTiersView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddEtiquetteTiersView view = (AddEtiquetteTiersView) target;
		super.validate(view, errors);

		if (StringUtils.isBlank(view.getCodeEtiquette())) {
			errors.rejectValue("codeEtiquette", "error.champ.obligatoire");
		}
	}
}
