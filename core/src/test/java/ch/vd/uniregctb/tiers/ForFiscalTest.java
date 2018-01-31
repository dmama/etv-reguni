package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForFiscalTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final ForFiscal forFiscal = new ForFiscalPrincipalPP();
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
