package ch.vd.unireg.tiers.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;

import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.view.CreateDebiteurView;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodiciteDecompte;

import static org.junit.Assert.assertFalse;

public class CreateDebiteurViewValidatorTest {

	private CreateDebiteurViewValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new CreateDebiteurViewValidator(ibanValidator);
	}

	/**
	 * [SIFISC-30948] Ce test s'assure qu'il est possible de créer une autre communauté avec des coordonnées financières complétement vides (= pas de coordonnées financières initiales)
	 */
	@Test
	public void testValidateDebiteurSansCoordoonneesFinancieres() {

		CreateDebiteurView view = new CreateDebiteurView();
		view.getFiscal().setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		view.getFiscal().setModeCommunication(ModeCommunication.PAPIER);
		view.getFiscal().setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}

}