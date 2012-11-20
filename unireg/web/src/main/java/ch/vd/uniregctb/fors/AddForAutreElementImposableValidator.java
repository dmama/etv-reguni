package ch.vd.uniregctb.fors;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class AddForAutreElementImposableValidator extends AddForRevenuFortuneValidator {

	public AddForAutreElementImposableValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForAutreElementImposableView.class.equals(clazz);
	}
}
