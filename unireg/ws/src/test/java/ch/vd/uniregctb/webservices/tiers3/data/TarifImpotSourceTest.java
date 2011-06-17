package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


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
