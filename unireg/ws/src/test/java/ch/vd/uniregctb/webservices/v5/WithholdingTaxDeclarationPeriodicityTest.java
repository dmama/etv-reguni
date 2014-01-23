package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class WithholdingTaxDeclarationPeriodicityTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxDeclarationPeriodicity.class, ch.vd.uniregctb.type.PeriodiciteDecompte.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.PeriodiciteDecompte) null));
		assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.MENSUEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.YEARLY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.ANNUEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.TRIMESTRIEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.SEMESTRIEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.ONCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte.UNIQUE));
	}
}
