package ch.vd.unireg.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidationException;
import ch.vd.unireg.iban.IbanValidator;

public abstract class AbstractCoordonneesFinancieresValidator {

	protected void validateDateFin(Errors errors, RegDate dateDebut, RegDate dateFin) {
		if (dateFin != null) {
			if (dateFin.isAfter(RegDate.get())) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
			if (dateDebut != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
		}
	}

	protected void validateAdresseBicSwift(Errors errors, String adresseBicSwift, String s) {
		if (StringUtils.isBlank(adresseBicSwift)) {
			errors.rejectValue("adresseBicSwift", s);
		}
	}

	protected void validateIBAN(Errors errors, String iban, IbanValidator ibanValidator) {
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
		else {
			errors.rejectValue("iban", "error.iban.mandat.tiers.vide");
		}
	}

	protected void validateDateDebut(Errors errors, RegDate dateDebut) {
		if (dateDebut == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		if (dateDebut != null && dateDebut.isAfter(RegDate.get())) {
			errors.rejectValue("dateDebut", "error.date.debut.future");
		}
	}
}
