package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.tiers.view.EntrepriseCivilView;

public class EntrepriseCivilViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseCivilView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EntrepriseCivilView view = (EntrepriseCivilView) target;

			// la raison sociale ne doit pas être vide
			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.raison.sociale.vide");
			}

			RegDate dateCreation = null;
			try {
				dateCreation = RegDateHelper.displayStringToRegDate(view.getsDateCreation(), true);
			} catch (Exception e) {
				// exception traitée plus bas
			}

			// Date de création
			if (StringUtils.isNotBlank(view.getsDateCreation())) {
				if (dateCreation == null || dateCreation.isAfter(RegDate.get())) {
					errors.rejectValue("sDateCreation", "error.date.creation.invalide");
				}
			} else {
				errors.rejectValue("sDateCreation", "error.date.creation.vide");
			}

			// Siège
			if (view.getNumeroOfsSiege() == null) {
				errors.rejectValue("numeroOfsSiege", "error.champ.obligatoire");
			}

			// la forme juridique doit être renseignée
			if (view.getFormeJuridique() == null) {
				errors.rejectValue("formeJuridique", "error.champ.obligatoire");
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
}
