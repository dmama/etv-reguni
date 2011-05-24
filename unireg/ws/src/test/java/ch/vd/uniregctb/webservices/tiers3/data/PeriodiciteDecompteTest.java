package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class PeriodiciteDecompteTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(PeriodiciteDecompte.class, ch.vd.uniregctb.type.PeriodiciteDecompte.class);
		assertEnumConstantsEqual(PeriodiciteDecompte.class, ch.vd.uniregctb.type.PeriodiciteDecompte.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.PeriodiciteDecompte) null));
		assertEquals(PeriodiciteDecompte.MENSUEL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.MENSUEL));
		assertEquals(PeriodiciteDecompte.ANNUEL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.ANNUEL));
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.TRIMESTRIEL));
		assertEquals(PeriodiciteDecompte.SEMESTRIEL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.SEMESTRIEL));
		assertEquals(PeriodiciteDecompte.UNIQUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.UNIQUE));
	}
}
