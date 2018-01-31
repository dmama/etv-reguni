package ch.vd.uniregctb.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.uniregctb.type.ModeCommunication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CommunicationModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(CommunicationMode.class, ModeCommunication.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (ModeCommunication mode : ModeCommunication.values()) {
			assertNotNull(mode.name(), EnumHelper.coreToWeb(mode));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ModeCommunication) null));
		assertEquals(CommunicationMode.WEB_SITE, EnumHelper.coreToWeb(ModeCommunication.SITE_WEB));
		assertEquals(CommunicationMode.UPLOAD, EnumHelper.coreToWeb(ModeCommunication.ELECTRONIQUE));
		assertEquals(CommunicationMode.PAPER, EnumHelper.coreToWeb(ModeCommunication.PAPIER));
	}
}
