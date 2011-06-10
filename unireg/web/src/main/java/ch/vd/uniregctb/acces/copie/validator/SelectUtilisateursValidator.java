package ch.vd.uniregctb.acces.copie.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.acces.copie.view.SelectUtilisateursView;

public class SelectUtilisateursValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SelectUtilisateursView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		SelectUtilisateursView selectUtilisateursView = (SelectUtilisateursView) obj;
		if(selectUtilisateursView.getNumeroUtilisateurReference() == null) {
			errors.rejectValue("utilisateurReference", "error.utilisateur.reference.vide");
		}
		if(selectUtilisateursView.getNumeroUtilisateurDestination() == null) {
			errors.rejectValue("utilisateurDestination", "error.utilisateur.destination.vide");
		}
	}

}
