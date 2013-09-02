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

		final UpiPersonInfo infoNonVide = service.getPersonInfo("7568409992270");
		Assert.assertNotNull(infoNonVide);
		Assert.assertEquals("7568409992270", infoNonVide.getNoAvs13());
	}
}
