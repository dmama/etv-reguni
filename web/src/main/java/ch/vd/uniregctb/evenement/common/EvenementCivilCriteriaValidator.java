package ch.vd.uniregctb.evenement.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.evenement.common.view.EvenementCivilCriteriaView;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class EvenementCivilCriteriaValidator implements Validator  {

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return EvenementCivilCriteriaView.class.isAssignableFrom(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 */
	@Override
	public void validate(Object target, Errors errors) {
		EvenementCivilCriteriaView bean = (EvenementCivilCriteriaView) target;

		// Numero CTB
		if (StringUtils.isNotBlank(bean.getNumeroCTBFormatte()) && !ValidatorUtils.isNumber(bean.getNumeroCTBFormatte())) {
			errors.rejectValue("numeroCTBFormatte", "error.numero");
		}

		// Numero Individu
		if (StringUtils.isNotBlank(bean.getNumeroIndividuFormatte()) && !ValidatorUtils.isNumber(bean.getNumeroIndividuFormatte())) {
			errors.rejectValue("numeroIndividuFormatte", "error.numero");
		}

	}

}
