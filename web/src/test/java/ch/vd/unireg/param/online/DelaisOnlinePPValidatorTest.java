package ch.vd.unireg.param.online;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;

import static org.junit.Assert.assertFalse;

public class DelaisOnlinePPValidatorTest {

	private DelaisOnlinePPValidator validator;
	private PeriodeFiscaleDAO periodeFiscaleDAO;

	@Before
	public void setUp() throws Exception {
		periodeFiscaleDAO = Mockito.mock(PeriodeFiscaleDAO.class);
		validator = new DelaisOnlinePPValidator();
		validator.setPeriodeFiscaleDAO(periodeFiscaleDAO);
	}

	/**
	 * [FISCPROJ-1077] Vérifie que le validateur ne crashe pas s'il n'y a pas du tout de période.
	 */
	@Test
	public void testValidateViewSansPeriode() {

		final PeriodeFiscale pf = new PeriodeFiscale();
		Mockito.when(periodeFiscaleDAO.get(1L)).thenReturn(pf);

		final DelaisOnlinePPView view = new DelaisOnlinePPView();
		view.setPeriodeFiscaleId(1L);
		view.setPeriodes(null);     // <-- pas du tout de pérides
		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");

		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}
}