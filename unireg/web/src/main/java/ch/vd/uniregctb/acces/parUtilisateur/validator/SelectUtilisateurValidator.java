package ch.vd.uniregctb.acces.parUtilisateur.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.acces.parUtilisateur.view.SelectUtilisateurView;

public class SelectUtilisateurValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SelectUtilisateurView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		SelectUtilisateurView selectUtilisateurView = (SelectUtilisateurView) obj;
		if(selectUtilisateurView.getNumeroUtilisateur() == null) {
			errors.rejectValue("utilisateur", "error.utilisateur.vide");
		}
	}

}
