package ch.vd.uniregctb.fors;

import org.springframework.orm.hibernate3.HibernateTemplate;

public class EditForAutreElementImposableValidator extends EditForRevenuFortuneValidator {

	public EditForAutreElementImposableValidator(HibernateTemplate hibernateTemplate) {
		super(hibernateTemplate);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForAutreElementImposableView.class.equals(clazz);
	}
}
