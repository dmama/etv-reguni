package ch.vd.unireg.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditRaisonEnseigneEtablissementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditRaisonEnseigneEtablissementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditRaisonEnseigneEtablissementView view = (EditRaisonEnseigneEtablissementView) target;

			if (view.getTiersId() == null) {
				errors.rejectValue("tiersId", "error.tiers.inexistant");
			}

			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.tiers.raison.sociale.vide");
			}
		}
	}
}
