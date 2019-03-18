package ch.vd.unireg.webservice.v7;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.ws.security.v7.AllowedAccess;
import ch.vd.unireg.ws.security.v7.PartyAccess;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.common.v1.UserLogin;

public class WebServiceSecurityItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceSecurityItTest.xml";

	private static boolean alreadySetUp = false;

	private static final UserLogin zaiptf = new UserLogin("zaiptf", 22); // Francis Perroset
	private static final UserLogin zaizzt = new UserLogin("zaizzt", 22); // Utilisateur technique taxation
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
		return Pair.of("/security/{user}/{partyNo}", map);
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

	private void doTestBatch(UserLogin login, AllowedAccess expected, MediaType mediaType, long... noTiers) {
		final Map<String, Object> params = new HashMap<>();
		params.put("user", login.getUserId());
		final String partyNosParams = Arrays.stream(noTiers)
				.mapToObj(no -> "partyNo=" + no)
				.collect(Collectors.joining("&"));
		final ResponseEntity<SecurityListResponse> response = get(SecurityListResponse.class, mediaType, "/securityOnParties?user={user}&" + partyNosParams, params);
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		final SecurityListResponse body = response.getBody();
		Assert.assertNotNull(body);
		Assert.assertEquals(login.getUserId(), body.getUser());
		final Map<Integer, AllowedAccess> accesses = body.getPartyAccesses().stream()
				.filter(p -> p.getAllowedAccess() != null)
				.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
		for (long no : noTiers) {
			Assert.assertEquals(expected, accesses.get((int) no));
		}
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
	 * Teste que Francis Perroset possède les droits d'écriture sur tous les contribuables (parce que les droits d'accès ont été définis comme ça)
	 */
	@Test
	public void testGetBatchSecurityFrancisPerroset() throws Exception {
		doTestBatch(zaiptf, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L, 10149508L);
		doTestBatch(zaiptf, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L, 10149508L);
	}

	/**
	 * Teste que l'utilisateur technique de taxation qui possède les rôles IZPOUDP et IZPOUDM peut bien accéder en lecture et en écriture sur tous les contribuables
	 */
	@Test
	public void testGetSecurityUserTechniqueTaxation() throws Exception {

		// Christine Schmid
		doTest(zaizzt, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 12300001L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Laurent Schmid
		doTest(zaizzt, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 12300002L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Christine & Laurent Schmid
		doTest(zaizzt, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 86006202L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Eric Cuendet
		doTest(zaizzt, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 10210315L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Jean-Philippe Maillefer
		doTest(zaizzt, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 61615502L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);

		// Pascal Broulis
		doTest(zaizzt, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML);
		doTest(zaizzt, 10149508L, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON);
	}

	/**
	 * Teste que l'utilisateur technique de taxation qui possède les rôles IZPOUDP et IZPOUDM peut bien accéder en lecture et en écriture sur tous les contribuables
	 */
	@Test
	public void testGetBatchSecurityUserTechniqueTaxation() throws Exception {
		doTestBatch(zaizzt, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L, 10149508L);
		doTestBatch(zaizzt, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L, 10149508L);
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
	 * Teste que Daniel Di Lallo ne possède aucun droit sur Laurent Schmid et son couple (interdiction) ni sur Pascal Broulis
	 * (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testGetBatchSecurityDanielDiLallo() throws Exception {
		doTestBatch(zciddo, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML, 12300001L, 10210315L, 61615502L);
		doTestBatch(zciddo, AllowedAccess.NONE, MediaType.APPLICATION_XML, 12300002L, 86006202L, 10149508L);
		doTestBatch(zciddo, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON, 12300001L, 10210315L, 61615502L);
		doTestBatch(zciddo, AllowedAccess.NONE, MediaType.APPLICATION_JSON, 12300002L, 86006202L, 10149508L);
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

	/**
	 * Teste que Pascal Mutrux (un employé de l'ACI pris au hazard) ne possède aucun droit sur Pascal Broulis (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testGetBatchSecurityRoselyneFavre() throws Exception {
		doTestBatch(zaipmx, AllowedAccess.READ_WRITE, MediaType.APPLICATION_XML, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L);
		doTestBatch(zaipmx, AllowedAccess.NONE, MediaType.APPLICATION_XML, 10149508L);
		doTestBatch(zaipmx, AllowedAccess.READ_WRITE, MediaType.APPLICATION_JSON, 12300001L, 12300002L, 86006202L, 10210315L, 61615502L);
		doTestBatch(zaipmx, AllowedAccess.NONE, MediaType.APPLICATION_JSON, 10149508L);
	}
}
