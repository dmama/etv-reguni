package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.complements.AddCoordonneesFinancieresValidator;
import ch.vd.unireg.complements.ComplementsEditCommunicationsValidator;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateNonHabitantView;

public class CreateNonHabitantViewValidator implements Validator {

	private final NonHabitantCivilViewValidator civilViewValidator;
	private final ComplementsEditCommunicationsValidator cpltCommViewValidator;
	private final AddCoordonneesFinancieresValidator cpltCoordFinViewValidator;

	public CreateNonHabitantViewValidator(IbanValidator ibanValidator) {
		this.civilViewValidator = new NonHabitantCivilViewValidator();
		this.cpltCommViewValidator = new ComplementsEditCommunicationsValidator();
		this.cpltCoordFinViewValidator = new AddCoordonneesFinancieresValidator(ibanValidator);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return CreateNonHabitantView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final CreateNonHabitantView view = (CreateNonHabitantView) target;

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
			if (!view.getComplementCoordFinanciere().isEmpty()) {   // [SIFISC-30948] lors de la création d'un tiers, les coordonnées financières ne sont pas obligatoires
				cpltCoordFinViewValidator.validate(view.getComplementCoordFinanciere(), errors);
			}
			if (errors.getErrorCount() > errorsBefore) {
				errors.reject("onglet.error.complements");
			}
		}
		finally {
			errors.popNestedPath();
		}
	}
}
