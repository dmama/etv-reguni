package ch.vd.unireg.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.identification.contribuable.view.IdentificationContribuableListCriteria;

public class IdentificationMessagesListValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return IdentificationContribuableListCriteria.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		if (!(obj instanceof IdentificationContribuableListCriteria)) {
			throw new IllegalArgumentException();
		}
		IdentificationContribuableListCriteria identificationContribuableListCriteria = (IdentificationContribuableListCriteria) obj;
	}
}