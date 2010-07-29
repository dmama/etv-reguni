package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;


public class ModeCommunicationTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(ModeCommunication.class, ch.vd.uniregctb.type.ModeCommunication.class);
		assertEnumConstantsEqual(ModeCommunication.class, ch.vd.uniregctb.type.ModeCommunication.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.ModeCommunication) null));
		assertEquals(ModeCommunication.SITE_WEB, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.SITE_WEB));
		assertEquals(ModeCommunication.ELECTRONIQUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.ELECTRONIQUE));
		assertEquals(ModeCommunication.PAPIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.PAPIER));
	}
}
