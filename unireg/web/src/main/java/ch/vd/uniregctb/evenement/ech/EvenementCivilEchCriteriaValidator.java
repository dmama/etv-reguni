package ch.vd.uniregctb.evenement.ech;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.evenement.common.EvenementCivilCriteriaValidator;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchCriteriaView;

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
