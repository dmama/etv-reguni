package ch.vd.unireg.fourreNeutre;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.fourreNeutre.view.FourreNeutreView;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FourreNeutreControllerValidatorTest extends WebTest {

	private FourreNeutreControllerValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validator = new FourreNeutreControllerValidator();
		validator.setTiersDAO(tiersDAO);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSelectionnerPeriode() throws Exception {

		final Long id = doInNewTransaction(status -> {
			PersonnePhysique pp = addNonHabitant("Eric", "Masserey", date(1976, 3, 12), Sexe.MASCULIN);
			return pp.getId();
		});

		FourreNeutreView view = new FourreNeutreView();
		view.setTiersId(id);
		view.setPeriodeFiscale(null);

		final Errors errors = new BeanPropertyBindingResult(view, "view");
		validator.validate(view, errors);
		final List<ObjectError> allErrors = errors.getAllErrors();
		assertNotNull(allErrors);
		assertEquals(1, allErrors.size());

		final ObjectError error = allErrors.get(0);
		assertEquals("error.fourre.neutre.periode.inconnu", error.getCode());

	}


}
