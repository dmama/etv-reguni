package ch.vd.unireg.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditSecteurActiviteEntrepriseViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditSecteurActiviteEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditSecteurActiviteEntrepriseView view = (EditSecteurActiviteEntrepriseView) target;

			if (view.getTiersId() == null) {
				errors.rejectValue("tiersId", "error.tiers.inexistant");
			}

			if (StringUtils.isBlank(view.getSecteurActivite())) {
				errors.rejectValue("secteurActivite", "error.tiers.secteur.activite.vide");
			}
		}
	}
}
