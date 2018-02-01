package ch.vd.unireg.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.debtor.v2.CommunicationMode;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class CommunicationModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(CommunicationMode.class, ch.vd.unireg.type.ModeCommunication.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.ModeCommunication) null));
		assertEquals(CommunicationMode.WEB_SITE, EnumHelper.coreToWeb(ch.vd.unireg.type.ModeCommunication.SITE_WEB));
		assertEquals(CommunicationMode.UPLOAD, EnumHelper.coreToWeb(ch.vd.unireg.type.ModeCommunication.ELECTRONIQUE));
		assertEquals(CommunicationMode.PAPER, EnumHelper.coreToWeb(ch.vd.unireg.type.ModeCommunication.PAPIER));
	}
}
