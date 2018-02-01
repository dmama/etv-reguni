package ch.vd.unireg.webservice.v5;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.xml.common.v1.UserLogin;

public class WebServiceRepaymentItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceRepaymentItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParamsForBlocked(UserLogin userLogin, long partyNumber) {
		final Map<String, Object> map = new HashMap<>();
		map.put("partyNo", partyNumber);
		map.put("user", userLogin.getUserId());
		map.put("oid", userLogin.getOid());
		return Pair.<String, Map<String, ?>>of("/repayment/{partyNo}/blocked?user={user}/{oid}", map);
	}

	@Test
	public void testBlocageFlag() throws Exception {

		final UserLogin user = new UserLogin("zaizzp", 22);
		final long noTiers = 12100003L;

		// état avant toute modification -> pas bloqué
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final ResponseEntity<String> resp = get(String.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertEquals("false", resp.getBody());
		}

		// blocage
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final HttpStatus status = put(params.getLeft(), params.getRight(), Boolean.TRUE, MediaType.APPLICATION_JSON);
			Assert.assertEquals(HttpStatus.OK, status);
		}

		// nouvel état -> bloqué
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final ResponseEntity<String> resp = get(String.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertEquals("true", resp.getBody());
		}

		// déblocage
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final HttpStatus status = put(params.getLeft(), params.getRight(), Boolean.FALSE, MediaType.APPLICATION_JSON);
			Assert.assertEquals(HttpStatus.OK, status);
		}

		// nouvel état -> débloqué
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final ResponseEntity<String> resp = get(String.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertEquals("false", resp.getBody());
		}

		// encore déblocage -> toujours débloqué
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final HttpStatus status = put(params.getLeft(), params.getRight(), Boolean.FALSE, MediaType.APPLICATION_JSON);
			Assert.assertEquals(HttpStatus.OK, status);
		}

		// état conservé -> débloqué
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParamsForBlocked(user, noTiers);
			final ResponseEntity<String> resp = get(String.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertEquals("false", resp.getBody());
		}
	}
}
