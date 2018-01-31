package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.tiers.view.AutreCommunauteCivilView;

public class AutreCommunauteCivilViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AutreCommunauteCivilView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AutreCommunauteCivilView view = (AutreCommunauteCivilView) target;

			// le nom ne doit pas être vide
			if (StringUtils.isBlank(view.getNom())) {
				errors.rejectValue("nom", "error.tiers.nom.vide");
			}

			// la forme juridique doit être renseignée
			if (view.getFormeJuridique() == null) {
				errors.rejectValue("formeJuridique", "error.champ.obligatoire");
			}

			// et le numéro IDE doit être valide si renseigné
			if (StringUtils.isNotBlank(view.getIde())) {
				if (!NumeroIDEHelper.isValid(view.getIde())) {
					errors.rejectValue("ide", "error.ide");
				}
			}
		}
	}
}
