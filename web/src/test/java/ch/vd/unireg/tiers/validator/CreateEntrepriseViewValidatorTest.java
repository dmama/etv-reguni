package ch.vd.unireg.tiers.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateEntrepriseView;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

import static org.junit.Assert.assertFalse;

public class CreateEntrepriseViewValidatorTest {

	private CreateEntrepriseViewValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new CreateEntrepriseViewValidator(ibanValidator);
	}

	/**
	 * [SIFISC-30948] Ce test s'assure qu'il est possible de créer une autre communauté avec des coordonnées financières complétement vides (= pas de coordonnées financières initiales)
	 */
	@Test
	public void testValidateEntrepriseSansCoordoonneesFinancieres() {

		CreateEntrepriseView view = new CreateEntrepriseView();
		view.getCivil().setRaisonSociale("Entreprise");
		view.getCivil().setFormeJuridique(FormeJuridiqueEntreprise.SA);
		view.getCivil().setNumeroOfsSiege(1);
		view.getCivil().setDateOuverture(RegDate.get(2000, 1, 1));

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}

}