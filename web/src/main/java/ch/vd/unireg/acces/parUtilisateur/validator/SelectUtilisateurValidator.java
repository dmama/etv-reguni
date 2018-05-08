package ch.vd.unireg.acces.parUtilisateur.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.acces.parUtilisateur.view.SelectUtilisateurView;

public class SelectUtilisateurValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SelectUtilisateurView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		SelectUtilisateurView selectUtilisateurView = (SelectUtilisateurView) obj;
		if(StringUtils.isBlank(selectUtilisateurView.getVisaOperateur())) {
			errors.rejectValue("utilisateur", "error.utilisateur.vide");
		}
	}

}
