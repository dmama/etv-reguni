package ch.vd.unireg.webservice.v5;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

public class WebServiceDebtorInfoItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceDebtorInfoItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(int partyNo, int pf) {
		final Map<String, Object> map = new HashMap<>();
		map.put("partyNo", partyNo);
		map.put("pf", pf);
		return Pair.<String, Map<String, ?>>of("/debtor/{partyNo}/{pf}?user=zaizze/22", map);
	}

	@Test
	public void testSimple() throws Exception {

		final int noTiers = 12500001;

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, 2009);
			final ResponseEntity<DebtorInfo> resp = get(DebtorInfo.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final DebtorInfo body = resp.getBody();
			Assert.assertEquals(noTiers, body.getNumber());
			Assert.assertEquals(2009, body.getTaxPeriod());
			Assert.assertEquals(1, body.getNumberOfWithholdingTaxDeclarationsIssued());
			Assert.assertEquals(12, body.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, 2009);
			final ResponseEntity<DebtorInfo> resp = get(DebtorInfo.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final DebtorInfo body = resp.getBody();
			Assert.assertEquals(noTiers, body.getNumber());
			Assert.assertEquals(2009, body.getTaxPeriod());
			Assert.assertEquals(1, body.getNumberOfWithholdingTaxDeclarationsIssued());
			Assert.assertEquals(12, body.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
	}
}
