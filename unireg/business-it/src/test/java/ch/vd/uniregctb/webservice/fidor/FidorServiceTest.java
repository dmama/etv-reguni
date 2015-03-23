package ch.vd.uniregctb.webservice.fidor;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.utils.UniregPropertiesImpl;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;

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
	public void testGetRue() throws Exception {
		final FidorClient wc = buildClient();
		final Street rue = wc.getRue(null, 1134510);       // Avenue de Longemalle, Renens VD
		Assert.assertNotNull(rue);
		Assert.assertEquals("Avenue de Longemalle", rue.getLongName());
	}

	@Test
	public void testGetRueInexistante() throws Exception {
		final FidorClient wc = buildClient();
		final Street rue = wc.getRue(null, 0);       // ???
		Assert.assertNull(rue);
	}

	@Test
	public void testGetRues() throws Exception {
		final FidorClient wc = buildClient();
		final List<Street> streets = wc.getRues(null, MockLocalite.Renens.getNoOrdre());
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
		final PostalLocality localite = wc.getLocalitePostale(null, MockLocalite.Renens.getNoOrdre());
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
		Collections.sort(localities, new Comparator<PostalLocality>() {
			@Override
			public int compare(PostalLocality o1, PostalLocality o2) {
				return o1.getLongName().compareTo(o2.getLongName());
			}
		});

		Assert.assertEquals("Echallens", localities.get(0).getLongName());
		Assert.assertEquals("St-Barthélemy VD", localities.get(1).getLongName());
		Assert.assertEquals("Villars-le-Terroir", localities.get(2).getLongName());
	}

	private FidorClientImpl buildClient() throws Exception {
		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.fidor.url");
		final FidorClientImpl client = new FidorClientImpl();
		client.setServiceUrl(rcpUrl);
		client.setUsername("gvd0unireg");
		client.setPassword("Welc0me_");
		return client;
	}

}
