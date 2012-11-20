package ch.vd.uniregctb.fors;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

public class AddForSecondaireValidator extends AddForRevenuFortuneValidator {

	public AddForSecondaireValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ForFiscalSecondaire.class.equals(clazz);
	}
}
