package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.EnumTest;
import ch.vd.uniregctb.webservices.tiers2.data.PeriodeDecompte;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class PeriodeDecompteTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(PeriodeDecompte.class, ch.vd.uniregctb.type.PeriodeDecompte.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.PeriodeDecompte) null));
		assertEquals(PeriodeDecompte.M01, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M01));
		assertEquals(PeriodeDecompte.M02, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M02));
		assertEquals(PeriodeDecompte.M03, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M03));
		assertEquals(PeriodeDecompte.M04, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M04));
		assertEquals(PeriodeDecompte.M05, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M05));
		assertEquals(PeriodeDecompte.M06, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M06));
		assertEquals(PeriodeDecompte.M07, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M07));
		assertEquals(PeriodeDecompte.M08, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M08));
		assertEquals(PeriodeDecompte.M09, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M09));
		assertEquals(PeriodeDecompte.M10, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M10));
		assertEquals(PeriodeDecompte.M11, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M11));
		assertEquals(PeriodeDecompte.M12, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.M12));
		assertEquals(PeriodeDecompte.T1, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.T1));		
		assertEquals(PeriodeDecompte.T2, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.T2));
		assertEquals(PeriodeDecompte.T3, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.T3));
		assertEquals(PeriodeDecompte.T4, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.T4));
		assertEquals(PeriodeDecompte.S1, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.S1));		
		assertEquals(PeriodeDecompte.S2, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.S2));		
		assertEquals(PeriodeDecompte.A, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte.A));
	}

}
