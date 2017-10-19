package ch.vd.uniregctb.webservice.rcpers;

import ch.ech.ech0085.v1.GetInfoPersonResponse;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.utils.UniregPropertiesImpl;

public class RcPersUpiServiceTest {

	private UniregProperties uniregProperties;

	public RcPersUpiServiceTest() {
		try {
			final UniregPropertiesImpl impl = new UniregPropertiesImpl();
			impl.setFilename("../base/unireg-ut.properties");
			impl.afterPropertiesSet();
			uniregProperties = impl;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testGetInfoPerson() throws Exception {
		final RcPersClientImpl client = buildClient();
		final GetInfoPersonResponse infoRefusee = client.getInfoPersonUpi(7560000000002L);
		Assert.assertNotNull(infoRefusee);
		Assert.assertNotNull(infoRefusee.getRefused());
		Assert.assertEquals(3, infoRefusee.getRefused().getReason());

		final GetInfoPersonResponse infoAcceptee = client.getInfoPersonUpi(7568409992270L);
		Assert.assertNotNull(infoAcceptee);
		Assert.assertNotNull(infoAcceptee.getAccepted());
		Assert.assertEquals(7568409992270L, infoAcceptee.getAccepted().getLatestAhvvn());
		Assert.assertNotNull(infoAcceptee.getAccepted().getValuesStoredUnderAhvvn());
	}

	private RcPersClientImpl buildClient() throws Exception {

		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.rcpers.url");
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl(rcpUrl);
		client.setUpiGetInfoPersonPath("upi/query/getInfoPerson");
		client.setUsername("gvd0unireg");
		client.setPassword("Welc0me_");
		return client;
	}
}
