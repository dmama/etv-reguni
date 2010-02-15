package ch.vd.uniregctb.tache.validator;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.utils.ValidateHelper;

public class TacheListValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TacheCriteriaView.class.equals(clazz) ;
	}

	public void validate(Object target, Errors errors) {

		TacheCriteriaView bean = (TacheCriteriaView) target;
		if (StringUtils.isNotBlank(bean.getNumeroFormate()) && ValidateHelper.isNumber(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroFormate())) == false) {
			errors.rejectValue("numeroFormate", "error.numero");
		}

	}

}
