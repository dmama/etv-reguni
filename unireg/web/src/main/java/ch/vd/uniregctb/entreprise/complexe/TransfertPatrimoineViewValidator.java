package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class TransfertPatrimoineViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return TransfertPatrimoineView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final TransfertPatrimoineView view = (TransfertPatrimoineView) target;

		if (view.getDateTransfert() == null) {
			errors.rejectValue("dateTransfert", "error.date.transfert.vide");
		}
		else if (view.getDateTransfert().isAfter(RegDate.get())) {
			errors.rejectValue("dateTransfert", "error.date.transfert.future");
		}
	}
}
