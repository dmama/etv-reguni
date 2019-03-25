package ch.vd.unireg.param.online;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.param.view.DelaisAccordablesOnlinePPView;
import ch.vd.unireg.type.DayMonth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

	/**
	 * [FISCPROJ-1077] Si la date de début est malformée, il ne faut pas afficher l'erreur 'date de début' obligatoire.
	 */
	@Test
	public void testValidateViewDateDebutMalformee() {

		final PeriodeFiscale pf = new PeriodeFiscale();
		Mockito.when(periodeFiscaleDAO.get(1L)).thenReturn(pf);

		final DelaisAccordablesOnlinePPView periode0 = new DelaisAccordablesOnlinePPView();
		periode0.setDelai1DemandeUnitaire(DayMonth.get(1, 1));
		periode0.setDelai1DemandeGroupee(DayMonth.get(1, 1));
		periode0.setDelai2DemandeUnitaire(DayMonth.get(3, 1));
		periode0.setDelai2DemandeGroupee(DayMonth.get(3, 1));

		final DelaisOnlinePPView view = new DelaisOnlinePPView();
		view.setPeriodeFiscaleId(1L);
		view.setPeriodes(Collections.singletonList(periode0));     // <-- pas du tout de pérides

		// on simule une erreur de parsing de la date de début
		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(view, "view");
		bindingResult.rejectValue("periodes[0].dateDebut", "typeMismatch.dateDebut");

		validator.validate(view, bindingResult);
		assertTrue(bindingResult.hasErrors());

		// on devrait avoir qu'une seule erreur : celle de l'erreur de parsing
		final List<ObjectError> errors = bindingResult.getAllErrors();
		assertNotNull(errors);
		assertEquals(1, errors.size());
		final FieldError error0 = (FieldError) errors.get(0);
		assertEquals("periodes[0].dateDebut", error0.getField());
		assertEquals("typeMismatch.dateDebut", error0.getCode());
	}
}