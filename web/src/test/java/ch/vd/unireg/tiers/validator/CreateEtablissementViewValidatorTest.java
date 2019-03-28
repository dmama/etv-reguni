package ch.vd.unireg.tiers.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateEtablissementView;

import static org.junit.Assert.assertFalse;

public class CreateEtablissementViewValidatorTest {

	private CreateEtablissementViewValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new CreateEtablissementViewValidator(ibanValidator);
	}

	/**
	 * [SIFISC-30948] Ce test s'assure qu'il est possible de créer une autre communauté avec des coordonnées financières complétement vides (= pas de coordonnées financières initiales)
	 */
	@Test
	public void testValidateEtablissementSansCoordoonneesFinancieres() {

		CreateEtablissementView view = new CreateEtablissementView();
		view.getCivil().setRaisonSociale("Etablissement");
		view.getCivil().setNoOfsCommune(1);
		view.getCivil().setDateDebut(RegDate.get(2000, 1, 1));
		view.getCivil().setsDateDebut("01.01.2000");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}

}