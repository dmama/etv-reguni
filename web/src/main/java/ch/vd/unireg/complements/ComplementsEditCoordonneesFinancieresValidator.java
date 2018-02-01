package ch.vd.unireg.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidationException;
import ch.vd.unireg.iban.IbanValidator;

public class ComplementsEditCoordonneesFinancieresValidator implements Validator {

	private final IbanValidator ibanValidator;

	public ComplementsEditCoordonneesFinancieresValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	@Override
	public boolean supports(Class clazz) {
		return ComplementsEditCoordonneesFinancieresView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final ComplementsEditCoordonneesFinancieresView view = (ComplementsEditCoordonneesFinancieresView) obj;

		final String iban = view.getIban();
		if (StringUtils.isNotBlank(iban)) {
			//[UNIREG-1449] il ne faudrait pas bloquer la sauvegarde de la page des "compléments" si l'IBAN, inchangé, est invalide.
			if (!IbanHelper.areSame(iban, view.getOldIban())) {
				try {
					ibanValidator.validate(iban);
				}
				catch (IbanValidationException e) {
					if (StringUtils.isBlank(e.getMessage())) {
						errors.rejectValue("iban", "error.iban");
					}
					else {
						errors.rejectValue("iban", "error.iban.detail", new Object[]{e.getMessage()}, "IBAN invalide");
					}
				}
			}
		}
	}
}
