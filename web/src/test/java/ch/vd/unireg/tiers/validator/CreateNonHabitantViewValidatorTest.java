package ch.vd.unireg.tiers.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateNonHabitantView;

import static org.junit.Assert.assertFalse;

public class CreateNonHabitantViewValidatorTest {

	private CreateNonHabitantViewValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new CreateNonHabitantViewValidator(ibanValidator);
	}

	/**
	 * [SIFISC-30948] Ce test s'assure qu'il est possible de créer un non-habitant avec des coordonnées financières complétement vides (= pas de coordonnées financières initiales)
	 */
	@Test
	public void testValidateNonHabitantSansCoordoonneesFinancieres() {

		CreateNonHabitantView view = new CreateNonHabitantView();
		view.getCivil().setNom("Nom");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}
}