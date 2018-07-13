package ch.vd.unireg.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidationException;
import ch.vd.unireg.iban.IbanValidator;

public class EditCoordonneesFinancieresValidator extends AbstractCoordonneesFinancieresValidator implements Validator {

	private final IbanValidator ibanValidator;

	public EditCoordonneesFinancieresValidator(IbanValidator ibanValidator) {
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
		validateDateFin( errors,  dateDebut,  view.getDateFin());

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
