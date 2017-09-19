package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;

import static ch.vd.uniregctb.validation.registrefoncier.EstimationRFValidatorTest.assertValide;

public class ModeleCommunauteRFValidatorTest {

	private ModeleCommunauteRFValidator validator;

	@Before
	public void setUp() throws Exception {
		validator = new ModeleCommunauteRFValidator();
	}

	@Test
	public void testModeleMembresNull() throws Exception {
		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(null);
		modele.setMembresHashCode(0);
		assertValide(validator.validate(modele));
	}

	@Test
	public void testModeleMembresVide() throws Exception {
		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(Collections.emptySet());
		modele.setMembresHashCode(1);
		assertValide(validator.validate(modele));
	}

	@Test
	public void testModeleMembresRenseignes() throws Exception {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);

		int hashCode = 31 * (31 + Long.valueOf(1L).hashCode()) + Long.valueOf(2L).hashCode();

		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(new HashSet<>(Arrays.asList(pp1, pp2)));
		modele.setMembresHashCode(hashCode);
		assertValide(validator.validate(modele));
	}
}