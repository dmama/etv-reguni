package ch.vd.unireg.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff;
import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class WithholdingTaxTariffTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxTariff.class, ch.vd.unireg.type.TarifImpotSource.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.TarifImpotSource) null));
		assertEquals(WithholdingTaxTariff.NORMAL, EnumHelper.coreToWeb(ch.vd.unireg.type.TarifImpotSource.NORMAL));
		assertEquals(WithholdingTaxTariff.DOUBLE_REVENUE, EnumHelper.coreToWeb(ch.vd.unireg.type.TarifImpotSource.DOUBLE_GAIN));
	}
}
