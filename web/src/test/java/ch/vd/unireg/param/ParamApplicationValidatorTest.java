package ch.vd.unireg.param;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.param.manager.ParamApplicationManager;
import ch.vd.unireg.param.validator.ParamApplicationValidator;
import ch.vd.unireg.param.view.ParamApplicationView;
import ch.vd.unireg.registrefoncier.allegement.EditDegrevementView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParamApplicationValidatorTest extends WebTest {

	private ParamApplicationValidator validator;
	private ParamApplicationManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validator = new ParamApplicationValidator();
		manager = getBean(ParamApplicationManager.class, "paramApplicationManager");
	}

	@Test
	public void testControleLocationRevenuTaille() throws Exception {

		ParamApplicationView view = manager.getForm();
		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(0, allErrors.size());
	}

}
