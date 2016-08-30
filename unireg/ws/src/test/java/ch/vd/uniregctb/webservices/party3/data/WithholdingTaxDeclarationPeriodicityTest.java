package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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
