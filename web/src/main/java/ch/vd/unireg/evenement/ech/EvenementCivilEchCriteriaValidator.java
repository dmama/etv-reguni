package ch.vd.unireg.evenement.ech;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.evenement.common.EvenementCivilCriteriaValidator;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchCriteriaView;

public class EvenementCivilEchCriteriaValidator extends EvenementCivilCriteriaValidator implements Validator  {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return EvenementCivilEchCriteriaView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		EvenementCivilEchCriteriaView bean = (EvenementCivilEchCriteriaView) target;

		if (bean.isModeLotEvenement() && StringUtils.isBlank(bean.getNumeroIndividuFormatte())) {
			errors.rejectValue("numeroIndividuFormatte", "error.champ.obligatoire");
		}

	}

}
