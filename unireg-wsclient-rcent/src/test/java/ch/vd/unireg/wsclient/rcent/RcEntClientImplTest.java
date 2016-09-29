package ch.vd.unireg.wsclient.rcent;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.TypeOfNoticeRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de tests utilitaires permettant de vérifier rapidement le bon fonctionnement du
 * client. Les tests nécessitant une connection à RCEnt sont en @Ignored par défaut.
 *
 * @author Raphaël Marmier, 2015-08-12
 */
public class RcEntClientImplTest {

	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-2.xsd",
			"eVD-0023-3-2.xsd",
			"eVD-0024-3-2.xsd"
	};

	private static final String BASE_URL = "http://rp-ws-va.etat-de-vaud.ch/registres/int-rcent/services";
	private static final String ORGANISATION_PATH = "/v3/organisation/CT.VD.PARTY";
	private static final String ORGANISATIONS_OF_NOTICE_PATH = "/v3/organisationsOfNotice";

	// Organisation cible pour les tests. Une seule suffit.
	private static final long NO100983251 = 100983251L;
	private static final String BOMACO_SÀRL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";
	private static final String NOTICE_REQUEST_VALIDATE_PATH = "/v3/noticeRequestValidate";
	private static final String NOTICE_REQUEST_LIST_PATH = "/v3/noticeRequestList";

	private static final String UNIREG_USERNAME = "gvd0unireg";
	private static final String UNIREG_PASSWORD = "Welc0me_";

	@Ignore
	@Test
	public void testGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		assertNotNull(data);
		assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Ignore
	@Test
	public void testGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		assertNotNull(data);
		assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	private RcEntClient createRCEntClient(boolean validating) throws Exception {
		RcEntClientImpl client = new RcEntClientImpl();
		client.setBaseUrl(BASE_URL);
		client.setUsername(UNIREG_USERNAME);
		client.setPassword(UNIREG_PASSWORD);
		client.setOrganisationPath(ORGANISATION_PATH);
		client.setOrganisationsOfNoticePath(ORGANISATIONS_OF_NOTICE_PATH);
		client.setNoticeRequestValidatePath(NOTICE_REQUEST_VALIDATE_PATH);
		client.setNoticeRequestListPath(NOTICE_REQUEST_LIST_PATH);
		if (validating) {
			client.setSchemasLocations(Arrays.asList(RCENT_SCHEMA));
			client.setValidationEnabled(true);
		}
		client.afterPropertiesSet();
		return client;
	}

	@Test
	public void testParseSimpleError() throws Exception {
		final String erreurSimple = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<eVD-0004:errors xmlns:eVD-0004=\"http://evd.vd.ch/xmlns/eVD-0004/3\"><eVD-0004:error><eVD-0004:code>100</eVD-0004:code><eVD-0004:message>Exception : catégorie de l'identifiant non valide : ct.vd.party</eVD-0004:message></eVD-0004:error></eVD-0004:errors>\n";

		final RcEntClientImpl rcEntClient = new RcEntClientImpl();
		rcEntClient.afterPropertiesSet();
		final List<RcEntClientErrorMessage> extracted = rcEntClient.parseErrors(erreurSimple);

		assertNotNull(extracted);
		final RcEntClientErrorMessage error = extracted.get(0);
		assertNotNull(error);
		assertEquals(100, error.getCode().intValue());
		assertEquals("Exception : catégorie de l'identifiant non valide : ct.vd.party", error.getMessage());
	}

	@Test
	public void testParseMultipleMultilineErrors() throws Exception {
		final String erreurMultipleMultiline = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<eVD-0004:errors xmlns:eVD-0004=\"http://evd.vd.ch/xmlns/eVD-0004/3\">\n  <eVD-0004:error>\n    <eVD-0004:code>100</eVD-0004:code>\n    <eVD-0004:message>Exception : catégorie de l'identifiant non valide : ct.vd.party</eVD-0004:message>\n  </eVD-0004:error>\n\n" +
				"  <eVD-0004:error>\n" +
				"    <eVD-0004:code>101</eVD-0004:code>\n" +
				"    <eVD-0004:message>Deuxième message d'erreur!</eVD-0004:message>\n" +
				"  </eVD-0004:error>\n</eVD-0004:errors>\n";

		final RcEntClientImpl rcEntClient = new RcEntClientImpl();
		rcEntClient.afterPropertiesSet();
		final List<RcEntClientErrorMessage> extracted = rcEntClient.parseErrors(erreurMultipleMultiline);

		assertNotNull(extracted);
		{
			final RcEntClientErrorMessage error = extracted.get(0);
			assertNotNull(error);
			assertEquals(100, error.getCode().intValue());
			assertEquals("Exception : catégorie de l'identifiant non valide : ct.vd.party", error.getMessage());
		}
		{
			final RcEntClientErrorMessage error = extracted.get(1);
			assertNotNull(error);
			assertEquals(101, error.getCode().intValue());
			assertEquals("Deuxième message d'erreur!", error.getMessage());
		}
	}

	@Test
	public void testExceptionPasErreurEmptyList() {
		final RcEntClientException exception = new RcEntClientException("Exception de test", null);
		assertNotNull(exception.getErrors());
		assertEquals(0, exception.getErrors().size());
	}

	/**
	 * Ce test vérifie que la mécanique de recherche des demandes d'annonce fonctionne bien correctement.
	 */
	@Test
	public void testFindNotices() throws Exception {

		final RcEntClient client = createRCEntClient(false);
		final RcEntNoticeQuery query = new RcEntNoticeQuery();
		query.setType(TypeOfNoticeRequest.CREATION);

		final Page<NoticeRequestReport> page = client.findNotices(query, new Sort.Order(Sort.Direction.ASC, "status"), 1, 5);
		assertNotNull(page);
		// TODO (msi) il n'y a pas actuellement (le 29.09.2016) de données stables en intégration, ce test se limite donc à vérifier que l'appel ne crashe pas. A améliorer quand il y aura des données stables.
		assertEquals(5, page.getSize());
	}
}