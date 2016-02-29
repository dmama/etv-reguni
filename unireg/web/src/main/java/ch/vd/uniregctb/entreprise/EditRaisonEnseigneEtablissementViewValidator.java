package ch.vd.uniregctb.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditRaisonEnseigneEtablissementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditRaisonEnseigneEtablissementView view = (EditRaisonEnseigneEtablissementView) target;

			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.tiers.raison.sociale.vide");
			}
		}
	}
}
