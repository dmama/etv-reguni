package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.person.v1.Sex;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SexeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(Sex.class, ch.vd.uniregctb.type.Sexe.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.Sexe) null));
		assertEquals(Sex.MALE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.Sexe.MASCULIN));
		assertEquals(Sex.FEMALE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.Sexe.FEMININ));
	}
}
