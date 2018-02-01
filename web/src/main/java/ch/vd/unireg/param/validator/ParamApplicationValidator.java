package ch.vd.unireg.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.param.view.ParamApplicationView;
import ch.vd.unireg.parametrage.ParametreEnum;
import ch.vd.unireg.parametrage.ParametreEnum.ValeurInvalideException;

public class ParamApplicationValidator implements Validator {
	
	private static final Logger L = LoggerFactory.getLogger(ParamApplicationValidator.class);

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParamApplicationView.class);
	}

	@Override
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
				throw new RuntimeException("Erreur lors de la lecture de la propriété '" + p.name() + '\'', e);
			}
		}
	}

}
