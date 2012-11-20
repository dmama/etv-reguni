package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

public class AddForPrincipalValidator extends AddForRevenuFortuneValidator {

	public AddForPrincipalValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ForFiscalPrincipal.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForPrincipalView view = (AddForPrincipalView) target;

		if (view.getModeImposition() == null) {
			errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
		}
	}
}
