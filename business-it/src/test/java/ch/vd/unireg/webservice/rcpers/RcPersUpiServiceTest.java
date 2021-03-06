package ch.vd.unireg.webservice.rcpers;

import ch.ech.ech0085.v1.GetInfoPersonResponse;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.utils.UniregProperties;
import ch.vd.unireg.utils.UniregPropertiesImpl;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.wsclient.rcpers.v5.RcPersClientImpl;

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

	private RcPersClientImpl buildClient() {

		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.rcpers.url");
		final WebClientPool wcPool = new WebClientPool();
		wcPool.setBaseUrl(rcpUrl);
		wcPool.setUsername("gvd0unireg");
		wcPool.setPassword("Welc0me_");
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setWcPool(wcPool);
		client.setUpiGetInfoPersonPath("upi/query/getInfoPerson");
		return client;
	}
}
