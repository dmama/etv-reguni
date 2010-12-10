package ch.vd.uniregctb.validation;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BusinessTest;

public abstract class AbstractValidatorTest<T> extends BusinessTest {

	private EntityValidator<T> validator;

	@SuppressWarnings({"unchecked"})
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		validator = getBean(EntityValidator.class, getValidatorBeanName());
	}

	protected abstract String getValidatorBeanName();

	protected ValidationResults validate(T entity) {
		return validator.validate(entity);
	}
}
