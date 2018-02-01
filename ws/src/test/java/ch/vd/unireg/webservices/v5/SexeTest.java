package ch.vd.unireg.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v3.Sex;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SexeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(Sex.class, Sexe.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (Sexe s : Sexe.values()) {
			assertNotNull(s.name(), EnumHelper.coreToWeb(s));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((Sexe) null));
		assertEquals(Sex.MALE, EnumHelper.coreToWeb(Sexe.MASCULIN));
		assertEquals(Sex.FEMALE, EnumHelper.coreToWeb(Sexe.FEMININ));
	}
}
