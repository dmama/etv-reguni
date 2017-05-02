package ch.vd.uniregctb.webservice.v6;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.ws.security.v6.AllowedAccess;
import ch.vd.unireg.ws.security.v6.SecurityResponse;
import ch.vd.unireg.xml.common.v1.UserLogin;

public class WebServiceSecurityItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceSecurityItTest.xml";

	private static boolean alreadySetUp = false;

	private static final UserLogin zaiptf = new UserLogin("zaiptf", 22); // Francis Perroset
	private static final UserLogin zaimkd = new UserLogin("zaimkd", 22); // Marinette Kellenberger
	private static final UserLogin zciddo = new UserLogin("zciddo", 0);  // Daniel Di Lallo
	private static final UserLogin zaipmx = new UserLogin("zaipmx", 19); // Pascal Mutrux

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(UserLogin userLogin, long partyNumber) {
		final Map<String, Object> map = new HashMap<>();
		map.put("user", userLogin.getUserId());
		map.put("partyNo", partyNumber);
		return Pair.<String, Map<String, ?>>of("/security/{user}/{partyNo}", map);
	}

	private void doTest(UserLogin login, long noTiers, AllowedAccess expected, MediaType mediaType) {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(login, noTiers);
		final ResponseEntity<SecurityResponse> response = get(SecurityResponse.class, mediaType, params.getLeft(), params.getRight());
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		final SecurityResponse body = response.getBody();
		Assert.assertNotNull(body);
		Assert.assertEquals(noTiers, body.getPartyNo());
		Assert.assertEquals(login.getUserId(), body.getUser());
		Assert.assertEquals(expected, body.getAllowedAccess());
	}

	@Test
	public void testTiersInconnu() throws Exception {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(zaiptf, 0L);
		{
			final ResponseEntity<SecurityResponse> response = get(SecurityResponse.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		}
		{
			final ResponseEntity<SecurityResponse> response = get(SecurityResponse.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
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
	 * Teste que Marinette Kellenberger possède les droits d'écriture sur tous les contribuables (parce qu'elle fait partie de la direction de l'ACI)
	 */
	@Test
	public void testGetSecurityMarinetteKellenberger() throws Exception {

		// Christine Schmid
		doTest(zaimkd, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zaimkd, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zaimkd, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zaimkd, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zaimkd, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zaimkd, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaimkd, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);
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
	 * Teste que Pascal Mutrux (un employé de l'ACI pris au hazard) ne possède aucun droit sur Pascal Broulis (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testGetSecurityRoselyneFavre() throws Exception {

		// Christine Schmid
		doTest(zaipmx, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zaipmx, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zaipmx, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zaipmx, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zaipmx, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zaipmx, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_XML);
		doTest(zaipmx, 10149508L, AllowedAccess.NONE, MediaType.APPLICATION_JSON);
	}
}
