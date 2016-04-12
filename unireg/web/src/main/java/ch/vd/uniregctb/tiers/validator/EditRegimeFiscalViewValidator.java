package ch.vd.uniregctb.tiers.validator;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.tiers.view.EditRegimeFiscalView;

public class EditRegimeFiscalViewValidator extends AbstractRegimeFiscalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditRegimeFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditRegimeFiscalView view = (EditRegimeFiscalView) target;

		// présence des dates et cohérence entre elles
		if (view.getDateDebut() == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		else if (view.getDateFin() != null && view.getDateDebut().isAfter(view.getDateFin())) {
			errors.rejectValue("dateFin", "error.date.fin.avant.debut");
		}

		// dates dans le futur -> interdit
		final RegDate today = RegDate.get();
		if (view.getDateDebut() != null && today.isBefore(view.getDateDebut())) {
			errors.rejectValue("dateDebut", "error.date.debut.future");
		}
		if (view.getDateFin() != null && today.isBefore(view.getDateFin())) {
			errors.rejectValue("dateFin", "error.date.fin.dans.futur");
		}

		// la portée est obligatoire
		if (view.getPortee() == null) {
			errors.rejectValue("portee", "error.portee.obligatoire");
		}

		// le code du régime
		if (StringUtils.isBlank(view.getCode())) {
			errors.rejectValue("code", "error.type.regime.fiscal.obligatoire");
		}
		else {
			final Map<String, TypeRegimeFiscal> types = getMapRegimesFiscauxParCode();
			if (types.get(view.getCode()) == null) {
				errors.rejectValue("code", "error.type.regime.fiscal.invalide");
			}
		}
	}
}
