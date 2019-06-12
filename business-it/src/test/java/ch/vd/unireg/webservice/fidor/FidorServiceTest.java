package ch.vd.unireg.webservice.fidor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.utils.UniregProperties;
import ch.vd.unireg.utils.UniregPropertiesImpl;
import ch.vd.unireg.webservice.fidor.v5.FidorClient;
import ch.vd.unireg.webservice.fidor.v5.FidorClientImpl;
import ch.vd.unireg.wsclient.WebClientPool;

public class FidorServiceTest {

	private UniregProperties uniregProperties;

	public FidorServiceTest() {
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
	public void testGetRueParEstrid() throws Exception {
		final FidorClient wc = buildClient();
		final List<Street> rues = wc.getRuesParEstrid(1134510, RegDate.get());       // Avenue de Longemalle, Renens VD
		Assert.assertNotNull(rues);
		Assert.assertEquals(1, rues.size());
		Assert.assertEquals("Avenue de Longemalle", rues.get(0).getLongName());
	}

	@Test
	public void testGetRueHisto() throws Exception {
		final FidorClient wc = buildClient();
		final List<Street> rues = wc.getRuesParEstrid(1134510, null);       // Avenue de Longemalle, Renens VD
		Assert.assertNotNull(rues);

		Assert.assertTrue(rues.size() > 1);
		for (Street rue : rues) {
			Assert.assertEquals("Avenue de Longemalle", rue.getLongName());
		}
	}

	@Test
	public void testGetRueInexistante() throws Exception {
		final FidorClient wc = buildClient();
		final List<Street> rues = wc.getRuesParEstrid(0, RegDate.get());       // ???
		Assert.assertNull(rues);
	}

	@Test
	public void testGetRues() throws Exception {
		final FidorClient wc = buildClient();
		final List<Street> streets = wc.getRuesParNumeroOrdrePosteEtDate(165 /* c'est Renens */, null);
		Assert.assertNotNull(streets);
		Assert.assertTrue(streets.size() > 80);     // quand le test a été écrit, il y avait 94 rues à Renens VD

		final Map<Integer, Street> streetMap = new HashMap<>(streets.size());
		for (Street street : streets) {
			streetMap.put(street.getEstrid(), street);
		}
		Assert.assertEquals("Estrid en double ?", streets.size(), streetMap.size());
		Assert.assertNotNull(streetMap.get(1134510));
		Assert.assertEquals("Avenue de Longemalle", streetMap.get(1134510).getLongName());
	}

	@Test
	public void testGetLocalitePostale() throws Exception {
		final FidorClient wc = buildClient();
		final PostalLocality localite = wc.getLocalitePostale(null, 165);
		Assert.assertNotNull(localite);
		Assert.assertEquals("Renens VD", localite.getLongName());
	}

	@Test
	public void testGetLocalitePostaleInexistante() throws Exception {
		final FidorClient wc = buildClient();
		final PostalLocality localite = wc.getLocalitePostale(null, 0);     // ???
		Assert.assertNull(localite);
	}

	@Test
	public void testGetLocalitesPostales() throws Exception {
		final FidorClient wc = buildClient();
		final List<PostalLocality> localities = wc.getLocalitesPostales(null, 1040, null, null, null);
		Assert.assertNotNull(localities);
		Assert.assertEquals(3, localities.size());      // Villars-le-Terroir, Echallens, St-Barthélemy VD

		// ordre alphabétique
		Collections.sort(localities, (o1, o2) -> o1.getLongName().compareTo(o2.getLongName()));

		Assert.assertEquals("Echallens", localities.get(0).getLongName());
		Assert.assertEquals("St-Barthélemy VD", localities.get(1).getLongName());
		Assert.assertEquals("Villars-le-Terroir", localities.get(2).getLongName());
	}

	private FidorClientImpl buildClient() throws Exception {
		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.fidor.url");
		final WebClientPool wcPool = new WebClientPool();
		wcPool.setBaseUrl(rcpUrl);
		wcPool.setUsername("gvd0unireg");
		wcPool.setPassword("Welc0me_");
		final FidorClientImpl client = new FidorClientImpl();
		client.setWcPool(wcPool);
		return client;
	}

}
