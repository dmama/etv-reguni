package ch.vd.unireg.evenement.organisation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.evenement.common.EvenementCivilCriteriaValidator;
import ch.vd.unireg.evenement.organisation.view.EvenementOrganisationCriteriaView;

public class EvenementOrganisationCriteriaValidator extends EvenementCivilCriteriaValidator implements Validator  {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return EvenementOrganisationCriteriaView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		EvenementOrganisationCriteriaView bean = (EvenementOrganisationCriteriaView) target;

		if (bean.isModeLotEvenement() && StringUtils.isBlank(bean.getNumeroOrganisationFormatte())) {
			errors.rejectValue("numeroOrganisationFormatte", "error.champ.obligatoire");
		}

	}

}
