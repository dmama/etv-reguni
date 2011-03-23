package ch.vd.uniregctb.param.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;

public class ModeleFeuilleDocumentValidator implements Validator{
	
	

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ModeleFeuilleDocumentView.class);
	}

	public void validate(Object target, Errors errors) {
	
	}

}
