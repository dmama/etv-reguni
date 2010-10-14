package ch.vd.uniregctb.acces.parDossier.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;

public class DroitAccesEditValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DroitAccesView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {
		final DroitAccesView droitAccesView = (DroitAccesView) obj;
		if (droitAccesView.getNumeroUtilisateur() == null) {
			errors.rejectValue("utilisateur", "error.utilisateur.vide");
		}
	}

}
