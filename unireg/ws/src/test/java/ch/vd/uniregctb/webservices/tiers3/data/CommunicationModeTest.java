package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.CommunicationMode;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class CommunicationModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(CommunicationMode.class, ch.vd.uniregctb.type.ModeCommunication.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.ModeCommunication) null));
		assertEquals(CommunicationMode.WEB_SITE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.SITE_WEB));
		assertEquals(CommunicationMode.UPLOAD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.ELECTRONIQUE));
		assertEquals(CommunicationMode.PAPER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeCommunication.PAPIER));
	}
}
