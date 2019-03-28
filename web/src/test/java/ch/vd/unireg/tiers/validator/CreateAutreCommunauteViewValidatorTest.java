package ch.vd.unireg.tiers.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateAutreCommunauteView;
import ch.vd.unireg.type.FormeJuridique;

import static org.junit.Assert.assertFalse;

public class CreateAutreCommunauteViewValidatorTest {

	private CreateAutreCommunauteViewValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new CreateAutreCommunauteViewValidator(ibanValidator);
	}

	/**
	 * [SIFISC-30948] Ce test s'assure qu'il est possible de créer une autre communauté avec des coordonnées financières complétement vides (= pas de coordonnées financières initiales)
	 */
	@Test
	public void testValidateAutreCommunauteSansCoordoonneesFinancieres() {

		CreateAutreCommunauteView view = new CreateAutreCommunauteView();
		view.getCivil().setNom("Nom");
		view.getCivil().setFormeJuridique(FormeJuridique.ASS);

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}

}