package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.tiers.view.EntrepriseCivilView;

public class EntrepriseCivilViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseCivilView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		final EntrepriseCivilView view = (EntrepriseCivilView) target;

		// la raison sociale ne doit pas être vide
		if (StringUtils.isBlank(view.getRaisonSociale())) {
			errors.rejectValue("raisonSociale", "error.raison.sociale.vide");
		}

		// la forme juridique non plus
		if (view.getFormeJuridique() == null) {
			errors.rejectValue("formeJuridique", "error.forme.juridique.vide");
		}

		if (!errors.hasFieldErrors("dateOuverture") && view.getDateOuverture() == null) {
			errors.rejectValue("dateOuverture", "error.date.ouverture.vide");
		}

		// dates explicites demandées
		if (view.getTypeDateDebutExerciceCommercial() != EntrepriseCivilView.TypeDefautDate.DEFAULT) {
			if (!errors.hasFieldErrors("dateDebutExerciceCommercial") && view.getDateDebutExerciceCommercial() == null) {
				errors.rejectValue("dateDebutExerciceCommercial", "error.date.debut.exercice.commercial.vide");
			}
			else if (view.getDateDebutExerciceCommercial() != null && view.getDateOuverture() != null && view.getDateDebutExerciceCommercial().isAfter(view.getDateOuverture())) {
				errors.rejectValue("dateDebutExerciceCommercial", "error.date.debut.exercice.commercial.apres.date.ouverture");
			}
		}
		if (view.getTypeDateFondation() != EntrepriseCivilView.TypeDefautDate.DEFAULT) {
			if (!errors.hasFieldErrors("dateFondation") && view.getDateFondation() == null) {
				errors.rejectValue("dateFondation", "error.date.fondation.vide");
			}
			else if (view.getDateFondation() != null && view.getDateOuverture() != null && view.getDateFondation().isAfter(view.getDateOuverture())) {
				errors.rejectValue("dateFondation", "error.date.fondation.apres.date.ouverture");
			}
		}

		// Siège
		if (view.getNumeroOfsSiege() == null) {
			errors.rejectValue("numeroOfsSiege", "error.champ.obligatoire");
		}

		// et le numéro IDE doit être valide si renseigné
		if (StringUtils.isNotBlank(view.getNumeroIde())) {
			if (!NumeroIDEHelper.isValid(view.getNumeroIde())) {
				errors.rejectValue("numeroIde", "error.ide");
			}
		}

		if (view.getCapitalLibere() != null) {
			if (StringUtils.isBlank(view.getDevise())) {
				errors.rejectValue("devise", "error.devise.obligatoire");
			}
		}
	}
}
