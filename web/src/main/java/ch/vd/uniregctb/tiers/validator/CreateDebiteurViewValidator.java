package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.complements.ComplementsEditCommunicationsValidator;
import ch.vd.uniregctb.complements.ComplementsEditCoordonneesFinancieresValidator;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.view.CreateDebiteurView;

public class CreateDebiteurViewValidator implements Validator {

	private final DebiteurFiscalViewValidator fiscalValidator;
	private final ComplementsEditCommunicationsValidator cpltCommViewValidator;
	private final ComplementsEditCoordonneesFinancieresValidator cpltCoordFinViewValidator;

	public CreateDebiteurViewValidator(IbanValidator ibanValidator) {
		this.fiscalValidator = new DebiteurFiscalViewValidator();
		this.cpltCommViewValidator = new ComplementsEditCommunicationsValidator();
		this.cpltCoordFinViewValidator = new ComplementsEditCoordonneesFinancieresValidator(ibanValidator);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return CreateDebiteurView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final CreateDebiteurView view = (CreateDebiteurView) target;

		errors.pushNestedPath("fiscal");
		try {
			final int errorsBefore = errors.getErrorCount();
			fiscalValidator.validate(view.getFiscal(), errors);
			if (errors.getErrorCount() > errorsBefore) {
				errors.reject("onglet.error.fiscal");
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
