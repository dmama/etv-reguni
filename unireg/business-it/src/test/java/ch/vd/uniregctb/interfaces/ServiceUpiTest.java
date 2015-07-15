package ch.vd.uniregctb.interfaces;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.uniregctb.common.BusinessItTest;

public class ServiceUpiTest extends BusinessItTest {

	private ServiceUpiRaw service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceUpiRaw.class, "serviceUpiRaw");
	}

	@Test
	public void testGetInfoPerson() throws Exception {
		final UpiPersonInfo infoVide = service.getPersonInfo("7560000000002");
		Assert.assertNull(infoVide);

		final UpiPersonInfo infoNonVideSansChangement = service.getPersonInfo("7568409992270");
		Assert.assertNotNull(infoNonVideSansChangement);
		Assert.assertEquals("7568409992270", infoNonVideSansChangement.getNoAvs13());

		final UpiPersonInfo infoNonVideAvecChangement = service.getPersonInfo("7561163512081");
		Assert.assertNotNull(infoNonVideAvecChangement);
		Assert.assertEquals("7564457068837", infoNonVideAvecChangement.getNoAvs13());
	}
}
