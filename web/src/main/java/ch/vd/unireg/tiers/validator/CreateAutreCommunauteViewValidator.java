package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.complements.AddCoordonneesFinancieresValidator;
import ch.vd.unireg.complements.ComplementsEditCommunicationsValidator;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateAutreCommunauteView;

public class CreateAutreCommunauteViewValidator implements Validator {

	private final AutreCommunauteCivilViewValidator civilViewValidator;
	private final ComplementsEditCommunicationsValidator cpltCommViewValidator;
	private final AddCoordonneesFinancieresValidator cpltCoordFinViewValidator;

	public CreateAutreCommunauteViewValidator(IbanValidator ibanValidator) {
		this.civilViewValidator = new AutreCommunauteCivilViewValidator();
		this.cpltCommViewValidator = new ComplementsEditCommunicationsValidator();
		this.cpltCoordFinViewValidator = new AddCoordonneesFinancieresValidator(ibanValidator);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return CreateAutreCommunauteView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final CreateAutreCommunauteView view = (CreateAutreCommunauteView) target;

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
