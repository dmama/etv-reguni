package ch.vd.unireg.tiers.validator;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.complements.CoordonneesFinancieresEditView;
import ch.vd.unireg.complements.EditCoordonneesFinancieresValidator;
import ch.vd.unireg.iban.IbanValidator;

public class EditCoordonneesFinanciereValidatorTest extends WebTestSpring3 {

	private EditCoordonneesFinancieresValidator validator;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final IbanValidator ibanValidator = getBean(IbanValidator.class, "ibanValidator");
		validator = new EditCoordonneesFinancieresValidator(ibanValidator);
	}


	@Test
	public void testIbanVide() throws Exception {

		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		final Errors errors = validate(view);
		Assert.assertNotNull(errors);
		Assert.assertEquals(1, errors.getFieldErrorCount());

		final FieldError error = errors.getFieldError("iban");
		Assert.assertNotNull(error);
		Assert.assertEquals("error.iban.mandat.tiers.vide", error.getCode());

	}

	/**
	 * Méthode de validation, retourne le binding results après passage de la validation
	 *
	 * @param view objet à valider
	 * @return binding results
	 */
	private Errors validate(final CoordonneesFinancieresEditView view) throws Exception {
		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		return errors;
	}

}
