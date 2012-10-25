package ch.vd.uniregctb.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;

public class IdentificationMessagesListValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return IdentificationMessagesListView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof IdentificationMessagesListView);
		IdentificationMessagesListView identificationMessagesListView = (IdentificationMessagesListView) obj;
	}
}