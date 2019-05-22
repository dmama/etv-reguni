package ch.vd.unireg.interfaces;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.upi.UpiConnector;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public class UpiConnectorTest extends BusinessItTest {

	private UpiConnector connector;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		connector = getBean(UpiConnector.class, "upiConnector");
	}

	@Test
	public void testGetInfoPerson() throws Exception {
		final UpiPersonInfo infoVide = connector.getPersonInfo("7560000000002");
		Assert.assertNull(infoVide);

		final UpiPersonInfo infoNonVideSansChangement = connector.getPersonInfo("7568409992270");
		Assert.assertNotNull(infoNonVideSansChangement);
		Assert.assertEquals("7568409992270", infoNonVideSansChangement.getNoAvs13());

		final UpiPersonInfo infoNonVideAvecChangement = connector.getPersonInfo("7561163512081");
		Assert.assertNotNull(infoNonVideAvecChangement);
		Assert.assertEquals("7564457068837", infoNonVideAvecChangement.getNoAvs13());
	}
}
