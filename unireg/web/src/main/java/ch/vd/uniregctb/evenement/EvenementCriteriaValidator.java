package ch.vd.uniregctb.evenement;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.evenement.view.EvenementCriteriaView;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class EvenementCriteriaValidator implements Validator  {

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return EvenementCriteriaView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 */
	@Override
	public void validate(Object target, Errors errors) {
		EvenementCriteriaView bean = (EvenementCriteriaView) target;

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
