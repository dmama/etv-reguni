package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

import static ch.vd.uniregctb.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EstimationRFValidatorTest {

	private EstimationRFValidator validator;

	@Before
	public void setUp() throws Exception {
		validator = new EstimationRFValidator();

	}

	/**
	 * Ce test vérifie que la validation des cas passants passe correctement.
	 */
	@Test
	public void testValidateCasPassant() throws Exception {
		assertValide(validator.validate(newEstimationRF(null, null)));
		assertValide(validator.validate(newEstimationRF(RegDate.get(2000, 1, 1), null)));
		assertValide(validator.validate(newEstimationRF(RegDate.get(2000, 1, 1), RegDate.get(2010, 1, 1))));
		assertValide(validator.validate(newEstimationRF(RegDate.get(2000, 1, 1), RegDate.get(2000, 1, 1))));
		assertValide(validator.validate(newEstimationRF(null, RegDate.get(2010, 1, 1))));
	}

	/**
	 * Ce test vérifie que la validation des cas non-passants lève bien des erreurs/warnings.
	 */
	@Test
	public void testValidateCasNonPassant() throws Exception {

		assertErrors(Collections.singletonList("L'estimation fiscale RF EstimationRF (? - ?) possède une date de début métier qui est après la date de fin métier: début = 01.01.2000, fin = 31.12.1999"),
		             validator.validate(newEstimationRF(RegDate.get(2000, 1, 1), RegDate.get(1999, 12, 31))));
	}

	/**
	 * Ce test vérifie que les estimations fiscales annulées sont toujours valides.
	 */
	@Test
	public void testValidateEstimationsAnnulees() throws Exception {

		final EstimationRF estimation = newEstimationRF(RegDate.get(2000, 1, 1), RegDate.get(1999, 12, 31));
		estimation.setAnnule(true);
		assertValide(validator.validate(estimation));
	}

	private static void assertValide(ValidationResults results) {
		assertNotNull(results);
		assertEmpty(results.getErrors());
		assertEmpty(results.getWarnings());
	}

	private static void assertErrors(List<String> errors, ValidationResults results) {
		assertNotNull(results);
		assertEquals(errors, results.getErrors());
		assertEquals(0, results.getWarnings().size());
	}

	@NotNull
	private static EstimationRF newEstimationRF(RegDate dateDebutMetier, RegDate dateFinMetier) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setDateDebutMetier(dateDebutMetier);
		estimation.setDateFinMetier(dateFinMetier);
		return estimation;
	}
}