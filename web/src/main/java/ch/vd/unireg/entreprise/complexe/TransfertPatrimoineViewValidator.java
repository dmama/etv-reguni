package ch.vd.unireg.entreprise.complexe;

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

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateTransfert")) {
			if (view.getDateTransfert() == null) {
				errors.rejectValue("dateTransfert", "error.date.transfert.vide");
			}
			else if (view.getDateTransfert().isAfter(RegDate.get())) {
				errors.rejectValue("dateTransfert", "error.date.transfert.future");
			}
		}
	}
}
