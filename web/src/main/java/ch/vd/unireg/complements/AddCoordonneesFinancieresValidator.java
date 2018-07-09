package ch.vd.unireg.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.iban.IbanValidator;

public class AddCoordonneesFinancieresValidator extends AbstractCoordonneesFinancieresValidator implements Validator {

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
		validateDateDebut(errors, view.getDateDebut());
		if (StringUtils.isBlank(view.getTitulaireCompteBancaire())) {
			errors.rejectValue("titulaireCompteBancaire", "error.titulaire.compte.tiers.vide");
		}
		validateAdresseBicSwift(errors, view.getAdresseBicSwift(), "error.bic.mandat.tiers.vide");
		validateDateFin(errors, view.getDateDebut(), view.getDateFin());
		validateIBAN(errors, view.getIban(), ibanValidator);
	}

}
