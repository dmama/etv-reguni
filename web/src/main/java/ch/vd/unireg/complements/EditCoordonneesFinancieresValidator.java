package ch.vd.unireg.complements;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
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

		validateDateFin(errors, view.getDateDebut(), view.getDateFin());
		validateIBAN(errors, view.getIban(), ibanValidator);
	}
}
