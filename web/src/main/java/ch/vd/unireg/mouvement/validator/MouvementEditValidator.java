package ch.vd.unireg.mouvement.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.type.Localisation;
import ch.vd.unireg.type.TypeMouvement;

public class MouvementEditValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return MouvementDetailView.class.equals(clazz);
	}

	@Override
	public void validate(Object obj, Errors errors) {
		MouvementDetailView mvtView = (MouvementDetailView) obj;
		if (mvtView.getTypeMouvement() == TypeMouvement.EnvoiDossier) {
			if (		(mvtView.getNumeroUtilisateurEnvoi() == null)
					&& 	(mvtView.getNoCollAdmDestinataireEnvoi() == null)) {
				errors.rejectValue("utilisateurEnvoi", "error.utilisateur.collectivite.vide");
			}
		}
		else if  (mvtView.getTypeMouvement() == TypeMouvement.ReceptionDossier) {
			if (		(mvtView.getLocalisation() == Localisation.PERSONNE)
					&& (mvtView.getNumeroUtilisateurReception() == null)) {
				errors.rejectValue("utilisateurReception", "error.utilisateur.vide");
			}
		}
	}
}