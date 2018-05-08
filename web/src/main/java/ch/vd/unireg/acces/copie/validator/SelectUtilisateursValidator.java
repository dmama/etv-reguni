package ch.vd.unireg.acces.copie.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.acces.copie.view.SelectUtilisateursView;

public class SelectUtilisateursValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SelectUtilisateursView.class.equals(clazz);
	}

	@Override
	public void validate(Object obj, Errors errors) {
		SelectUtilisateursView selectUtilisateursView = (SelectUtilisateursView) obj;
		if (StringUtils.isBlank(selectUtilisateursView.getVisaUtilisateurReference())) {
			errors.rejectValue("utilisateurReference", "error.utilisateur.reference.vide");
		}
		if (StringUtils.isBlank(selectUtilisateursView.getVisaUtilisateurDestination())) {
			errors.rejectValue("utilisateurDestination", "error.utilisateur.destination.vide");
		}
	}

}
