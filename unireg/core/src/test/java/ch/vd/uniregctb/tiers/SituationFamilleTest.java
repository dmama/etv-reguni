package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class SituationFamilleTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final SituationFamille situation = new SituationFamillePersonnePhysique();
		situation.setDateDebut(RegDate.get(2000, 1, 1));
		situation.setDateFin(RegDate.get(2009, 12, 31));

		situation.setAnnule(false);
		assertTrue(situation.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(situation.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(situation.isValidAt(RegDate.get(2060, 1, 1)));

		situation.setAnnule(true);
		assertFalse(situation.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(situation.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(situation.isValidAt(RegDate.get(2060, 1, 1)));
	}
}
