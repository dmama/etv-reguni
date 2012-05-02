package ch.vd.uniregctb.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.di.view.DelaiDeclarationView;

public class ListeRecapEditDelaiValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DelaiDeclarationView.class.equals(clazz);
	}

	@Override
	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof DelaiDeclarationView);
		DelaiDeclarationView delai = (DelaiDeclarationView) obj;
		if (delai.getDelaiAccordeAu() == null) {
			ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
		}
	}

}


