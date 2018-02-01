package ch.vd.unireg.webservice.v5;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.ws.search.party.v1.SearchResult;
import ch.vd.unireg.xml.party.v3.PartyType;

public class WebServiceSearchPartyItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceSearchPartyItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(Integer partyNo, String nom, Set<PartyType> types) {
		final StringBuilder b = new StringBuilder("/searchParty?user=zaiptf/22");
		final Map<String, Object> map = new HashMap<>();
		if (partyNo != null) {
			map.put("partyNo", partyNo);
			b.append("&partyNo={partyNo}");
		}
		if (StringUtils.isNotBlank(nom)) {
			map.put("name", nom);
			b.append("&name={name}");
		}
		if (types != null && !types.isEmpty()) {
			for (PartyType type : types) {
				b.append("&partyType=").append(type.name());
			}
		}
		return Pair.<String, Map<String, ?>>of(b.toString(), map);
	}

	@Test
	public void searchSansCritere() throws Exception {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(null, null, null);
		{
			final ResponseEntity<SearchResult> resp = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertNotNull(resp.getBody().getError());
			Assert.assertEquals("Les critères de recherche sont vides", resp.getBody().getError().getErrorMessage());
			Assert.assertNotNull(resp.getBody().getParty());
			Assert.assertEquals(0, resp.getBody().getParty().size());
		}
		{
			final ResponseEntity<SearchResult> resp = get(SearchResult.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
			Assert.assertNotNull(resp.getBody().getError());
			Assert.assertEquals("Les critères de recherche sont vides", resp.getBody().getError().getErrorMessage());
			Assert.assertNotNull(resp.getBody().getParty());
			Assert.assertEquals(0, resp.getBody().getParty().size());
		}
	}

	@Test
	public void searchByName() throws Exception {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(null, "Emery", null);
		final ResponseEntity<SearchResult> resp = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
		Assert.assertNotNull(resp);
		Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
		Assert.assertNull(resp.getBody().getError());
		Assert.assertNotNull(resp.getBody().getParty());
		Assert.assertEquals(2, resp.getBody().getParty().size());
	}

	@Test
	public void searchByTypeSansResultat() throws Exception {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(null, "Emery", EnumSet.of(PartyType.DEBTOR, PartyType.CORPORATION, PartyType.HOUSEHOLD));
		final ResponseEntity<SearchResult> resp = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
		Assert.assertNotNull(resp);
		Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
		if (resp.getBody().getError() != null) {
			Assert.fail(resp.getBody().getError().toString());
		}
		Assert.assertNotNull(resp.getBody().getParty());
		Assert.assertEquals(0, resp.getBody().getParty().size());
	}

	@Test
	public void searchByTypeAvecResultat() throws Exception {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(null, "Emery", EnumSet.of(PartyType.DEBTOR, PartyType.NATURAL_PERSON));
		final ResponseEntity<SearchResult> resp = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
		Assert.assertNotNull(resp);
		Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());
		if (resp.getBody().getError() != null) {
			Assert.fail(resp.getBody().getError().toString());
		}
		Assert.assertNotNull(resp.getBody().getParty());
		Assert.assertEquals(2, resp.getBody().getParty().size());
	}
}
