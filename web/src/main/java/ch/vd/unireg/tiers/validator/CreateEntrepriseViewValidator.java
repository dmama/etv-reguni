package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.complements.ComplementsEditCommunicationsValidator;
import ch.vd.unireg.complements.EditCoordonneesFinancieresValidator;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateEntrepriseView;

public class CreateEntrepriseViewValidator implements Validator {

	private final EntrepriseCivilViewValidator civilViewValidator;
	private final ComplementsEditCommunicationsValidator cpltCommViewValidator;
	private final EditCoordonneesFinancieresValidator cpltCoordFinViewValidator;

	public CreateEntrepriseViewValidator(IbanValidator ibanValidator) {
		this.civilViewValidator = new EntrepriseCivilViewValidator();
		this.cpltCommViewValidator = new ComplementsEditCommunicationsValidator();
		this.cpltCoordFinViewValidator = new EditCoordonneesFinancieresValidator(ibanValidator);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return CreateEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final CreateEntrepriseView view = (CreateEntrepriseView) target;

		errors.pushNestedPath("civil");
		try {
			final int errorsBefore = errors.getErrorCount();
			civilViewValidator.validate(view.getCivil(), errors);
			if (errors.getErrorCount() > errorsBefore) {
				errors.reject("onglet.error.civil");
			}
		}
		finally {
			errors.popNestedPath();
		}

		errors.pushNestedPath("complementCommunication");
		try {
			final int errorsBefore = errors.getErrorCount();
			cpltCommViewValidator.validate(view.getComplementCommunication(), errors);
			if (errors.getErrorCount() > errorsBefore) {
				errors.reject("onglet.error.complements");
			}
		}
		finally {
			errors.popNestedPath();
		}

		errors.pushNestedPath("complementCoordFinanciere");
		try {
			final int errorsBefore = errors.getErrorCount();
			cpltCoordFinViewValidator.validate(view.getComplementCoordFinanciere(), errors);
			if (errors.getErrorCount() > errorsBefore) {
				errors.reject("onglet.error.complements");
			}
		}
		finally {
			errors.popNestedPath();
		}
	}
}
