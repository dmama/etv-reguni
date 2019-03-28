package ch.vd.unireg.complements;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidationException;
import ch.vd.unireg.iban.IbanValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditCoordonneesFinancieresValidatorTest {

	private EditCoordonneesFinancieresValidator validator;
	private IbanValidator ibanValidator;

	@Before
	public void setUp() throws Exception {
		ibanValidator = Mockito.mock(IbanValidator.class);
		validator = new EditCoordonneesFinancieresValidator(ibanValidator);
	}

	/**
	 * Ce test s'assure qu'il est possible de modifier des coordonnées financières avec une date de début nulle
	 */
	@Test
	public void testValidateDateDebutNulle() throws IbanValidationException {

		// un vue avec une date de début nulle
		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		view.setDateDebut(null);
		view.setIban("IBAN valide");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}

	/**
	 * Ce test s'assure qu'il n'est pas possible de modifier des coordonnées financières avec une date de début dans le futur
	 */
	@Test
	public void testValidateDateDebutDansLeFutur() throws IbanValidationException {

		// un vue avec une date de début nulle
		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		view.setDateDebut(RegDate.get().addMonths(6));
		view.setIban("IBAN valide");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertTrue(bindingResult.hasErrors());

		final List<ObjectError> errors = bindingResult.getAllErrors();
		assertEquals(1, errors.size());

		final FieldError error0 = (FieldError) errors.get(0);
		assertEquals("error.date.debut.future", error0.getCode());
		assertEquals("dateDebut", error0.getField());
	}

	/**
	 * Ce test s'assure qu'il n'est pas possible de modifier des coordonnées financières avec des dates de début et de fin incohérentes
	 */
	@Test
	public void testValidateDateDebutEtFinIncoherentes() throws IbanValidationException {

		// un vue avec une date de fin avant la date de début
		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		view.setDateDebut(RegDate.get(2010, 1, 1));
		view.setDateFin(RegDate.get(2005, 12, 31));
		view.setIban("IBAN valide");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertTrue(bindingResult.hasErrors());

		final List<ObjectError> errors = bindingResult.getAllErrors();
		assertEquals(1, errors.size());

		final FieldError error0 = (FieldError) errors.get(0);
		assertEquals("error.date.fin.avant.debut", error0.getCode());
		assertEquals("dateFin", error0.getField());
	}

	/**
	 * Ce test s'assure qu'il n'est pas possible de modifier des coordonnées financières avec un IBAN invalide
	 */
	@Test
	public void testValidateIbanInvalide() throws IbanValidationException {

		Mockito.doThrow(new IbanValidationException("exception de test")).when(ibanValidator).validate(Mockito.anyString());

		// un vue avec un IBAN invalide
		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		view.setDateDebut(RegDate.get(2000, 1, 1));
		view.setIban("turlututu");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertTrue(bindingResult.hasErrors());

		final List<ObjectError> errors = bindingResult.getAllErrors();
		assertEquals(1, errors.size());

		final FieldError error0 = (FieldError) errors.get(0);
		assertEquals("error.iban.detail", error0.getCode());
		assertEquals("iban", error0.getField());
	}

	/**
	 * Ce test s'assure qu'il est possible de modifier des coordonnées financières avec un IBAN invalide, du moment qu'il n'a pas été changé
	 */
	@Test
	public void testValidateIbanInvalideSansChangement() throws IbanValidationException {

		Mockito.doThrow(new IbanValidationException("exception de test")).when(ibanValidator).validate(Mockito.anyString());

		// un vue avec un IBAN invalide
		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		view.setDateDebut(RegDate.get(2000, 1, 1));
		view.setOldIban("turlututu");
		view.setIban("turlututu");  // <-- l'IBAN n'a pas été changé

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, bindingResult);
		assertFalse(bindingResult.hasErrors());
	}
}