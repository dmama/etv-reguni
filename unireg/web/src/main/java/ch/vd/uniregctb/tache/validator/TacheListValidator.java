package ch.vd.uniregctb.tache.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class TacheListValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TacheCriteriaView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object target, Errors errors) {

		TacheCriteriaView bean = (TacheCriteriaView) target;
		if (StringUtils.isNotBlank(bean.getNumeroFormate()) && !ValidatorUtils.isNumber(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroFormate()))) {
			errors.rejectValue("numeroFormate", "error.numero");
		}

	}

}
