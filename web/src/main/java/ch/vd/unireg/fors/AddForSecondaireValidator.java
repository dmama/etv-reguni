package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AddForSecondaireValidator extends AddForRevenuFortuneValidator {

	public AddForSecondaireValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService, hibernateTemplate);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForSecondaireView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForSecondaireView view = (AddForSecondaireView) target;

		if (view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
		}
	}
}
