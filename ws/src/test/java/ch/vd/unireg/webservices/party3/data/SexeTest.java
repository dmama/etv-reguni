package ch.vd.unireg.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v1.Sex;
import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SexeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(Sex.class, ch.vd.unireg.type.Sexe.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.Sexe) null));
		assertEquals(Sex.MALE, EnumHelper.coreToWeb(ch.vd.unireg.type.Sexe.MASCULIN));
		assertEquals(Sex.FEMALE, EnumHelper.coreToWeb(ch.vd.unireg.type.Sexe.FEMININ));
	}
}
