package ch.vd.unireg.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.corporation.v5.TaxSystemScope;
import ch.vd.unireg.tiers.RegimeFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaxSystemScopeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(TaxSystemScope.class, RegimeFiscal.Portee.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (RegimeFiscal.Portee s : RegimeFiscal.Portee.values()) {
			assertNotNull(s.name(), EnumHelper.coreToWeb(s));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((RegimeFiscal.Portee) null));
		assertEquals(TaxSystemScope.CH, EnumHelper.coreToWeb(RegimeFiscal.Portee.CH));
		assertEquals(TaxSystemScope.VD, EnumHelper.coreToWeb(RegimeFiscal.Portee.VD));
	}
}
