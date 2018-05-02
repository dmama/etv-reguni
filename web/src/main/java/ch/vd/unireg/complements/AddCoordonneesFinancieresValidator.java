package ch.vd.unireg.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidationException;
import ch.vd.unireg.iban.IbanValidator;

public class AddCoordonneesFinancieresValidator implements Validator {

	private final IbanValidator ibanValidator;

	public AddCoordonneesFinancieresValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	@Override
	public boolean supports(Class clazz) {
		return CoordonneesFinancieresEditView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final CoordonneesFinancieresEditView view = (CoordonneesFinancieresEditView) obj;

		// validation de la plage de validité
		final RegDate dateDebut = view.getDateDebut();
		if (dateDebut != null && dateDebut.isAfter(RegDate.get())) {
			errors.rejectValue("dateDebut", "error.date.debut.future");
		}

		final RegDate dateFin = view.getDateFin();
		if (dateFin != null) {
			if (dateFin.isAfter(RegDate.get())) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
			if (dateDebut != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
		}

		final String iban = view.getIban();
		if (StringUtils.isNotBlank(iban)) {
			// l'iban doit être valide lorsqu'on ajoute de nouvelles coordonnées financières
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
