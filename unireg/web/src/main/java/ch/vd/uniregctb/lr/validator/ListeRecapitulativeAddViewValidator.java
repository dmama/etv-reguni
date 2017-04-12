package ch.vd.uniregctb.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeAddView;

public class ListeRecapitulativeAddViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ListeRecapitulativeAddView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ListeRecapitulativeAddView view = (ListeRecapitulativeAddView) target;
		if (!errors.hasFieldErrors("delaiAccorde")) {
			if (view.getDelaiAccorde() == null) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.vide");
			}
			else if (RegDate.get().isAfter(view.getDelaiAccorde())) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.anterieure.date.jour");
			}
		}
	}
}
