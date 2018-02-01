package ch.vd.uniregctb.webservices.v6;

import org.junit.Test;

import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class WithholdingTaxDeclarationPeriodicityTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxDeclarationPeriodicity.class, ch.vd.uniregctb.type.PeriodiciteDecompte.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (PeriodiciteDecompte pd : PeriodiciteDecompte.values()) {
			assertNotNull(pd.name(), EnumHelper.coreToWeb(pd));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((PeriodiciteDecompte) null));
		assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, EnumHelper.coreToWeb(PeriodiciteDecompte.MENSUEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.YEARLY, EnumHelper.coreToWeb(PeriodiciteDecompte.ANNUEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.QUARTERLY, EnumHelper.coreToWeb(PeriodiciteDecompte.TRIMESTRIEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, EnumHelper.coreToWeb(PeriodiciteDecompte.SEMESTRIEL));
		assertEquals(WithholdingTaxDeclarationPeriodicity.ONCE, EnumHelper.coreToWeb(PeriodiciteDecompte.UNIQUE));
	}
}
