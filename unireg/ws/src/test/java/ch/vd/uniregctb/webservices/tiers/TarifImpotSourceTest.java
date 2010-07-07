package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;


public class TarifImpotSourceTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(TarifImpotSource.class, ch.vd.uniregctb.type.TarifImpotSource.class);
		assertEnumConstantsEqual(TarifImpotSource.class, ch.vd.uniregctb.type.TarifImpotSource.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TarifImpotSource) null));
		assertEquals(TarifImpotSource.NORMAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TarifImpotSource.NORMAL));
		assertEquals(TarifImpotSource.DOUBLE_GAIN, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TarifImpotSource.DOUBLE_GAIN));
	}
}
