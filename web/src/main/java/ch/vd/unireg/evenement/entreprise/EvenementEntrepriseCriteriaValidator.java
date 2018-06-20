package ch.vd.unireg.evenement.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.evenement.common.EvenementCivilCriteriaValidator;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseCriteriaView;

public class EvenementEntrepriseCriteriaValidator extends EvenementCivilCriteriaValidator implements Validator  {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return EvenementEntrepriseCriteriaView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		EvenementEntrepriseCriteriaView bean = (EvenementEntrepriseCriteriaView) target;

		if (bean.isModeLotEvenement() && StringUtils.isBlank(bean.getNumeroOrganisationFormatte())) {
			errors.rejectValue("numeroOrganisationFormatte", "error.champ.obligatoire");
		}

	}

}
