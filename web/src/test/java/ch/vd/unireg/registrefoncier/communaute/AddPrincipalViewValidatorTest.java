package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.List;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.registrefoncier.allegement.AbstractEditDegrevementViewValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AddPrincipalViewValidatorTest extends WebTest {
	private AddPrincipalViewValidator validator;
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validator = new AddPrincipalViewValidator();
	}

	@Test
	public void testPeriodeDebutNull() {
		AddPrincipalView view = new AddPrincipalView();
		view.setPeriodeDebut(null);

		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.periode.fiscale.vide", error.getCode());
	}

	@Test
	public void testRangePeriode() {
		AddPrincipalView view = new AddPrincipalView();
		view.setPeriodeDebut(1850);

		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.param.annee", error.getCode());
	}

	@Test
	public void testPeriodeFiscaleFutur() {
		AddPrincipalView view = new AddPrincipalView();
		view.setPeriodeDebut(RegDate.get().year() + 2);

		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);

		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.principal.periode.fiscale", error.getCode());
	}

}
