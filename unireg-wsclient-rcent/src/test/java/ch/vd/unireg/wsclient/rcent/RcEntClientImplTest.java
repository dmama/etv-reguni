package ch.vd.unireg.wsclient.rcent;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.evd0022.v3.OrganisationData;

/**
 * Classe de tests utilitaires permettant de vérifier rapidement le bon fonctionnement du
 * client. Les tests nécessitant une connection à RCEnt sont en @Ignored par défaut.
 * @author Raphaël Marmier, 2015-08-12
 */
public class RcEntClientImplTest {

	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-0.xsd",
			"eVD-0023-3-0.xsd",
			"eVD-0024-3-0.xsd"
	};

	private static final String BASE_URL = "http://slv2737v.etat-de-vaud.ch:8040/services";
	private static final String ORGANISATION_PATH = "/v3/organisation/CT.VD.PARTY";
	private static final String ORGANISATIONS_OF_NOTICE_PATH = "/v3/organisationsOfNotice";

	// Organisation cible pour les tests. Une seule suffit.
	private static final long NO100983251 = 100983251L;
	private static final String BOMACO_SÀRL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";

	@Before
	public void setUp() throws Exception {
	}

	@Ignore
	@Test
	public void testGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Ignore
	@Test
	public void testGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	private RcEntClient createRCEntClient(boolean validating) throws Exception {
		RcEntClientImpl client = new RcEntClientImpl();
		client.setBaseUrl(BASE_URL);
		client.setOrganisationPath(ORGANISATION_PATH);
		client.setOrganisationsOfNoticePath(ORGANISATIONS_OF_NOTICE_PATH);
		if (validating) {
			client.setSchemasLocations(Arrays.asList(RCENT_SCHEMA));
			client.setValidationEnabled(true);
		}
		client.afterPropertiesSet();
		return client;
	}
}