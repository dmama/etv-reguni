package ch.vd.uniregctb.registrefoncier.allegement;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.uniregctb.common.WebTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbstractEditDegrevementViewValidatorTest extends WebTest {

	private AbstractEditDegrevementViewValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validator = new AbstractEditDegrevementViewValidator();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testControleRevenuTaille() throws Exception {

		EditDegrevementView view = new EditDegrevementView();
		// valeurs obligatoires
		view.setAnneeDebut(2009);
		view.getLocation().setPourcentageArrete(new BigDecimal(23));
		view.getPropreUsage().setPourcentageArrete(new BigDecimal(23));
		view.getLoiLogement().setPourcentageCaractereSocial(new BigDecimal(23));
		// valeurs testées
		view.getLocation().setRevenu(12345678901234567L);
		view.getLocation().setSurface(123456789012L);

		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.degexo.revenu.taille", error.getCode());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testControleSurfaceTaille() throws Exception {


		EditDegrevementView view = new EditDegrevementView();
		// valeurs obligatoires
		view.setAnneeDebut(2009);
		view.getLocation().setPourcentageArrete(new BigDecimal(23));
		view.getPropreUsage().setPourcentageArrete(new BigDecimal(23));
		view.getLoiLogement().setPourcentageCaractereSocial(new BigDecimal(23));
		// valeurs testées
		view.getLocation().setRevenu(123456789012L);
		view.getLocation().setSurface(1234567890123456L);

		Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.degexo.surface.taille", error.getCode());
	}

}
