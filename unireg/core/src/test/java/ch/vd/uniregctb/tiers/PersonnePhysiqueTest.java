package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersonnePhysiqueTest extends WithoutSpringTest {

	@Test
	public void testValidateNomVideNonHabitant() {

		PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom(null);
		assertTrue(pp.validate().hasErrors());

		pp.setNom("");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("  ");
		assertTrue(pp.validate().hasErrors());

		pp.setNom("Bob");
		assertFalse(pp.validate().hasErrors());
	}
}
