package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;

public class SexeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(Sexe.class, ch.vd.uniregctb.type.Sexe.class);
		assertEnumConstantsEqual(Sexe.class, ch.vd.uniregctb.type.Sexe.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.Sexe) null));
		assertEquals(Sexe.MASCULIN, EnumHelper.coreToWeb(ch.vd.uniregctb.type.Sexe.MASCULIN));
		assertEquals(Sexe.FEMININ, EnumHelper.coreToWeb(ch.vd.uniregctb.type.Sexe.FEMININ));
	}
}
