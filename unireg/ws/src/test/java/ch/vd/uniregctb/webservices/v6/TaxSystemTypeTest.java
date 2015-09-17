package ch.vd.uniregctb.webservices.v6;

import org.junit.Test;

import ch.vd.unireg.xml.party.corporation.v4.TaxSystemType;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaxSystemTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(TaxSystemType.class, TypeRegimeFiscal.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeRegimeFiscal s : TypeRegimeFiscal.values()) {
			assertNotNull(s.name(), EnumHelper.coreToWeb(s));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeRegimeFiscal) null));
		assertEquals(TaxSystemType.ORDINARY, EnumHelper.coreToWeb(TypeRegimeFiscal.ORDINAIRE));
	}
}
