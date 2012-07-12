package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ForFiscalTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final ForFiscal forFiscal = new ForFiscalPrincipal();
		forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
		forFiscal.setDateFin(RegDate.get(2009, 12, 31));

		forFiscal.setAnnule(false);
		assertTrue(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));

		forFiscal.setAnnule(true);
		assertFalse(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));
	}

}
