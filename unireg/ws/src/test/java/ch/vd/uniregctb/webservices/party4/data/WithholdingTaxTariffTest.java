package ch.vd.uniregctb.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v2.WithholdingTaxTariff;
import ch.vd.uniregctb.webservices.party4.EnumTest;
import ch.vd.uniregctb.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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
