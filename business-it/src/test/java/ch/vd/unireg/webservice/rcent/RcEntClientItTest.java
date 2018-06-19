package ch.vd.unireg.webservice.rcent;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntSchemaHelper;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientErrorMessage;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.unireg.wsclient.rcent.RcEntClientImpl;
import ch.vd.unireg.wsclient.rcent.RcEntNoticeQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RcEntClientItTest extends BusinessItTest {

	private static final String USER_ID = "unireg";
	private static final int RCENT_ERROR_NO_DATA_BEFORE = 9;
	private static final int RCENT_ERROR_NOT_FOUND = 2;

	private static final long ID_BCV = 101544776L;
	private static final String NOM_BCV = "Banque Cantonale Vaudoise";

	private WebClientPool wcPool;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final String username = uniregProperties.getProperty("testprop.webservice.rcent.username");
		final String password = uniregProperties.getProperty("testprop.webservice.rcent.password");
		final String baseUrl = uniregProperties.getProperty("testprop.webservice.rcent.url");

		wcPool = new WebClientPool();
		wcPool.setUsername(username);
		wcPool.setPassword(password);
		wcPool.setBaseUrl(baseUrl);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
		wcPool.close();
	}

	@Test(timeout = 30000)
	public void testGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testGetAnnonceIDE() throws Exception {

		final long ID_ANNONCE = 180007882L;

		final RcEntClient client = createRCEntClient(true);
		final RcEntNoticeQuery rcEntNoticeQuery = new RcEntNoticeQuery();
		rcEntNoticeQuery.setUserId(USER_ID);
		rcEntNoticeQuery.setNoticeId(ID_ANNONCE);
		final Page<NoticeRequestReport> pages = client.findNotices(rcEntNoticeQuery, null, 1, 10);
		assertNotNull(pages);
		assertEquals(1, pages.getTotalElements());
		final List<NoticeRequestReport> listOfNoticeRequest = pages.getContent();
		assertEquals(1, listOfNoticeRequest.size());
		assertEquals(Long.toString(ID_ANNONCE), listOfNoticeRequest.get(0).getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId());
	}


	//Notice not found
	@Test(timeout = 30000)
	public void testGetOrganisationOfNoticeNotFound() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		try {
			client.getOrganisationsOfNotice(206949858L, RcEntClient.OrganisationState.BEFORE);
			fail();
		}
		catch (RcEntClientException e) {
			assertNotNull(e.getErrors());
			assertEquals(1, e.getErrors().size());
			final RcEntClientErrorMessage rcEntClientErrorMessage = e.getErrors().get(0);
			//Erreur 404 evenement non trouvé
			assertEquals(RCENT_ERROR_NOT_FOUND, rcEntClientErrorMessage.getCode().intValue());
		}
	}

	//Erreur 400
	@Test(timeout = 30000)
	public void testGetOrganisationOfNoticeWithoutBefore() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		try {
			client.getOrganisationsOfNotice(819520L, RcEntClient.OrganisationState.BEFORE);
			fail();
		}
		catch (RcEntClientException e) {
			assertNotNull(e.getErrors());
			assertEquals(1, e.getErrors().size());
			final RcEntClientErrorMessage rcEntClientErrorMessage = e.getErrors().get(0);
			//Erreur 404 evenement non trouvé
			assertEquals(RCENT_ERROR_NO_DATA_BEFORE, rcEntClientErrorMessage.getCode().intValue());
		}
	}

	/**
	 * [SIFISCBS-83] Teste que le tri par défaut est bien par id de notification décroissant
	 */
	@Test(timeout = 30000)
	public void testDefaultTri() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		// the order
		final Sort.Order order = new Sort.Order(Sort.Direction.DESC, "noticeRequestId");
		// the query params
		RcEntNoticeQuery query = new RcEntNoticeQuery();

		Page<NoticeRequestReport> page = client.findNotices( query, order, 1, 20);

		// Il faut filtrer pour ne garder que les annonces IDE (pas REE)
		List<NoticeRequestReport> annoncesIDE = page.getContent().stream()
				.filter( a -> a.getNoticeRequest().getNoticeRequestHeader() != null && a.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification() != null)
				.collect(Collectors.toList());

		List<NoticeRequestReport> sortedList = annoncesIDE.stream().sorted((b, a) -> a.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId()
				.compareTo(b.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId())).collect(Collectors.toList());

		assertNotNull(page);

		// Vérification que l'ordre est bien par "notification id" décroissant
		NoticeRequestReport reports[] = annoncesIDE.toArray(new NoticeRequestReport[annoncesIDE.size()]);
		NoticeRequestReport sortedReports[] = sortedList.toArray(new NoticeRequestReport[annoncesIDE.size()]);

		String unsortedTabId = "";
		String sortedTabId = "";
		boolean isSameOrder = true;

		for( int i= 0; i<reports.length; i++ ) {
			unsortedTabId = reports[i].getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId();
			sortedTabId = sortedReports[i].getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId();
			if (!unsortedTabId.equals(sortedTabId)) {
				isSameOrder = false;
				break;
			}
		}

		assertTrue(isSameOrder);
	}

	/**
	 * [SIFISCBS-83] Teste qu'il est possible de trier sur une colonne du tableau: "date" croissant
	 */
	@Test(timeout = 30000)
	public void testTriDateAscColonne() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		// the order
		final Sort.Order order = new Sort.Order(Sort.Direction.ASC, "date");
		// the query params
		RcEntNoticeQuery query = new RcEntNoticeQuery();

		Page<NoticeRequestReport> page = client.findNotices( query, order, 1, 20);

		// Il faut filtrer pour ne garder que les annonces IDE (pas REE)
		List<NoticeRequestReport> annoncesIDE = page.getContent().stream()
				.filter( a -> a.getNoticeRequest().getNoticeRequestHeader() != null && a.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification() != null)
				.collect(Collectors.toList());

		List<NoticeRequestReport> sortedList = annoncesIDE.stream().sorted((a, b) -> a.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestDateTime()
				.compareTo(b.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestDateTime())).collect(Collectors.toList());

		assertNotNull(page);

		// Vérification que l'ordre est bien par "date" croissant
		NoticeRequestReport reports[] = annoncesIDE.toArray(new NoticeRequestReport[annoncesIDE.size()]);
		NoticeRequestReport sortedReports[] = sortedList.toArray(new NoticeRequestReport[annoncesIDE.size()]);

		Date unsortedTabDate;
		Date sortedTabDate;
		boolean isSameOrder = true;

		for( int i= 0; i<reports.length; i++ ) {
			unsortedTabDate = reports[i].getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestDateTime();
			sortedTabDate = sortedReports[i].getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestDateTime();
			if (!unsortedTabDate.equals(sortedTabDate)) {
				isSameOrder = false;
				break;
			}
		}

		assertTrue(isSameOrder);
	}

	private RcEntClient createRCEntClient(boolean validating) throws Exception {
		RcEntClientImpl client = new RcEntClientImpl();
		client.setWcPool(wcPool);
		if (validating) {
			client.setSchemasLocations(Arrays.asList(RCEntSchemaHelper.RCENT_SCHEMA));
			client.setValidationEnabled(true);
		}
		client.afterPropertiesSet();
		return client;
	}
}
