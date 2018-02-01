package ch.vd.unireg.acces.parDossier.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.acces.parDossier.view.DroitAccesView;

public class DroitAccesEditValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DroitAccesView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		final DroitAccesView droitAccesView = (DroitAccesView) obj;
		if (droitAccesView.getNumeroUtilisateur() == null) {
			errors.rejectValue("utilisateur", "error.utilisateur.vide");
		}
	}

}
