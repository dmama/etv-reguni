package ch.vd.uniregctb.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.parametrage.ParametreEnum.ValeurInvalideException;

public class ParamApplicationValidator implements Validator {
	
	private static final Logger L = Logger.getLogger(ParamApplicationValidator.class);

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParamApplicationView.class);
	}

	public void validate(Object objForm, Errors errors) {
		ParamApplicationView form =  (ParamApplicationView) objForm;
		if (ParamApplicationView.Action.reset == form.getAction()) {
			// On ne valide pas les champs du formulaire lors de la demande d'un reset
			return;
		}
		for(ParametreEnum p : ParametreEnum.values()) {
			try {
				p.validerValeur((String)PropertyUtils.getProperty(form, p.name()));
			} catch (ValeurInvalideException e) {
				errors.rejectValue(p.name(), "error.param." + p.getType());
			} catch (Exception e) {
				L.error(e.getMessage(), e);
				throw new RuntimeException("Erreur lors de la lecture de la propriété '" + p.name() + "'", e);
			}
		}
	}

}
