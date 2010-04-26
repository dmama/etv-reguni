package ch.vd.uniregctb.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.DelaiDeclaration;

public class ListeRecapEditDelaiValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DelaiDeclaration.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof DelaiDeclaration);
		DelaiDeclaration delai = (DelaiDeclaration) obj;
		if (delai.getDelaiAccordeAu() == null) {
			ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
		}
	}

}


