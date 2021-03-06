package ch.vd.unireg.fors;

import org.springframework.validation.Errors;

import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AddForAutreElementImposableValidator extends AddForRevenuFortuneValidator {

	public AddForAutreElementImposableValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService, hibernateTemplate);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForAutreElementImposableView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForAutreElementImposableView view = (AddForAutreElementImposableView) target;

		if (view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
		}
	}
}
