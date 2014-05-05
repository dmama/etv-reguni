package ch.vd.uniregctb.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.utils.ValidatorUtils;

public class ComplementsEditCommunicationsValidator implements Validator {

	@Override
	public boolean supports(Class clazz) {
		return ComplementsEditCommunicationsView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final ComplementsEditCommunicationsView view = (ComplementsEditCommunicationsView) obj;

		if (StringUtils.isNotBlank(view.getNumeroTelecopie())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelecopie())) {
				errors.rejectValue("numeroTelecopie", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephonePrive())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephonePrive())) {
				errors.rejectValue("numeroTelephonePrive", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephonePortable())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephonePortable())) {
				errors.rejectValue("numeroTelephonePortable", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephoneProfessionnel())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephoneProfessionnel())) {
				errors.rejectValue("numeroTelephoneProfessionnel", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getAdresseCourrierElectronique())) {
			if (!ValidatorUtils.isValidEmail(view.getAdresseCourrierElectronique())) {
				errors.rejectValue("adresseCourrierElectronique", "error.email");
			}
		}
	}
}
