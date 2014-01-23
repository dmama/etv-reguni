package ch.vd.uniregctb.webservice.v5;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import ch.vd.unireg.ws.security.v1.AllowedAccess;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.xml.common.v1.UserLogin;

public class WebServiceSecurityItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceSecurityItTest.xml";

	private static boolean alreadySetUp = false;

	private static final UserLogin zaiptf = new UserLogin("zaiptf", 22); // Francis Perroset
	private static final UserLogin zaipmd = new UserLogin("zaipmd", 22); // Philippe Maillard
	private static final UserLogin zciddo = new UserLogin("zciddo", 0);  // Daniel Di Lallo
	private static final UserLogin zairfa = new UserLogin("zairfa", 10); // Roselyne Favre

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private ResponseEntity<SecurityResponse> callService(UserLogin login, long noTiers, @NotNull MediaType mediaType) {
		final RestTemplate template = buildTemplateWithAcceptHeader(mediaType);
		try {
			final ResponseEntity<SecurityResponse> response = template.getForEntity(v5Url + "/security/{user}/{partyNo}", SecurityResponse.class, login.getUserId(), noTiers);
			Assert.assertNotNull(response);
			return response;
		}
		catch (HttpStatusCodeException e) {
			return new ResponseEntity<>(e.getStatusCode());
		}
	}

	private void doTest(UserLogin login, long noTiers, AllowedAccess expected, MediaType mediaType) {
		final ResponseEntity<SecurityResponse> response = callService(login, noTiers, mediaType);
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		final SecurityResponse body = response.getBody();
		Assert.assertNotNull(body);
		Assert.assertEquals(noTiers, body.getPartyNo());
		Assert.assertEquals(login.getUserId(), body.getUser());
		Assert.assertEquals(expected, body.getAllowedAccess());
	}

	@Test
	public void testTiersInconnu() throws Exception {
		{
			final ResponseEntity<SecurityResponse> response = callService(zaiptf, 0L, MediaType.APPLICATION_XML);
			Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		}
		{
			final ResponseEntity<SecurityResponse> response = callService(zaiptf, 0L, MediaType.APPLICATION_JSON);
			Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		}
	}

	/**
	 * Teste que Francis Perroset possède les droits d'écriture sur tous les contribuables (parce que les droits d'accès ont été définis comme ça)
	 */
	@Test
	public void testGetSecurityFrancisPerroset() throws Exception {

		// Christine Schmid
		doTest(zaiptf, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zaiptf, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zaiptf, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zaiptf, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zaiptf, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zaiptf, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaiptf, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);
	}

	/**
	 * Teste que Philippe Maillard possède les droits d'écriture sur tous les contribuables (parce qu'il fait partie de la direction de l'ACI)
	 */
	@Test
	public void testGetSecurityPhilippeMaillard() throws Exception {

		// Christine Schmid
		doTest(zaipmd, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zaipmd, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zaipmd, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zaipmd, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zaipmd, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zaipmd, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmd, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);
	}

	/**
	 * Teste que Daniel Di Lallo ne possède aucun droit sur Laurent Schmid et son couple (interdiction) ni sur Pascal Broulis
	 * (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testGetSecurityDanielDiLallo() throws Exception {

		// Christine Schmid
		doTest(zciddo, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zciddo, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zciddo, 12300002L, AllowedAccess.NONE, MediaType.APPLICATION_XML);
		doTest(zciddo, 12300002L, AllowedAccess.NONE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zciddo, 86006202L, AllowedAccess.NONE, MediaType.APPLICATION_XML);
		doTest(zciddo, 86006202L, AllowedAccess.NONE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zciddo, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zciddo, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zciddo, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zciddo, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zciddo, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_XML);
		doTest(zciddo, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_JSON);
	}

	/**
	 * Teste que Roselyne Favre (une employée de l'ACI prise au hazard) ne possède aucun droit sur Pascal Broulis (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testGetSecurityRoselyneFavre() throws Exception {

		// Christine Schmid
		doTest(zairfa, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zairfa, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zairfa, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zairfa, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zairfa, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zairfa, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zairfa, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zairfa, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zairfa, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zairfa, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zairfa, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_XML);
		doTest(zairfa, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_JSON);
	}
}
