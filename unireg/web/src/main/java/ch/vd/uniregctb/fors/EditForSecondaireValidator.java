package ch.vd.uniregctb.fors;

import org.springframework.orm.hibernate3.HibernateTemplate;

public class EditForSecondaireValidator extends EditForRevenuFortuneValidator {

	public EditForSecondaireValidator(HibernateTemplate hibernateTemplate) {
		super(hibernateTemplate);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForSecondaireView.class.equals(clazz);
	}
}
