package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class WithholdingTaxTariffTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxTariff.class, ch.vd.uniregctb.type.TarifImpotSource.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TarifImpotSource) null));
		assertEquals(WithholdingTaxTariff.NORMAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TarifImpotSource.NORMAL));
		assertEquals(WithholdingTaxTariff.DOUBLE_REVENUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TarifImpotSource.DOUBLE_GAIN));
	}
}
