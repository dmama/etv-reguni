package ch.vd.uniregctb.mouvement.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaSimpleEtatView;

// TODO (jde) implementer cette classe
public class MouvementMasseConsultationValidator implements Validator {

	public boolean supports(Class clazz) {
		return MouvementMasseCriteriaSimpleEtatView.class.equals(clazz);
	}

	public void validate(Object target, Errors errors) {
	}
}
