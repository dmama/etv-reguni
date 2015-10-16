package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.tiers.view.ContribuableInfosEntrepriseView;

public class ContribuableInfosEntrepriseViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ContribuableInfosEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final ContribuableInfosEntrepriseView view = (ContribuableInfosEntrepriseView)target;
			// et le numéro IDE doit être valide si renseigné
			if (StringUtils.isNotBlank(view.getIde())) {
				if (!NumeroIDEHelper.isValid(view.getIde())) {
					errors.rejectValue("ide", "error.ide");
				}
			}
		}
	}
}
