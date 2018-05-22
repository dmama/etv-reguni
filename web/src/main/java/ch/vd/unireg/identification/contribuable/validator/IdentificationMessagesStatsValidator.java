package ch.vd.unireg.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;

public class IdentificationMessagesStatsValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return IdentificationMessagesStatsCriteriaView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		if (!(obj instanceof IdentificationMessagesStatsCriteriaView)) {
			throw new IllegalArgumentException();
		}
		IdentificationMessagesStatsCriteriaView identificationMessagesStatsCriteriaView = (IdentificationMessagesStatsCriteriaView) obj;
	}
}