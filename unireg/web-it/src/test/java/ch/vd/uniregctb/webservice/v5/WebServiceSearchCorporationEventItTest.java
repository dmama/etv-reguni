package ch.vd.uniregctb.webservice.v5;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.ws.search.corpevent.v1.SearchResult;

/**
 * TODO [SIPM] Pour l'instant (?), les événements PM ne sont plus supportés
 */
@Ignore
public class WebServiceSearchCorporationEventItTest extends AbstractWebServiceItTest {

	private static Pair<String, Map<String, ?>> buildUriAndParams(@Nullable Integer pmId, @Nullable String eventCode, @Nullable RegDate from, @Nullable RegDate to) {
		final Map<String, Object> map = new HashMap<>();
		final StringBuilder b = new StringBuilder();
		if (pmId != null) {
			map.put("id", pmId);
			b.append("&corporationId={id}");
		}
		if (from != null) {
			map.put("from", RegDateHelper.dateToDisplayString(from));
			b.append("&startDay={from}");
		}
		if (to != null) {
			map.put("to", RegDateHelper.dateToDisplayString(to));
			b.append("&endDay={to}");
		}
		if (StringUtils.isNotBlank(eventCode)) {
			map.put("eventCode", eventCode);
			b.append("&eventCode={eventCode}");
		}
		return Pair.<String, Map<String, ?>>of(String.format("/searchCorporationEvent?user=zaizzt/22%s", b.toString()), map);
	}

	@Test
	public void testEmptyCriteria() throws Exception {
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, null, null, null);
			final ResponseEntity<SearchResult> res = get(SearchResult.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(res);
			Assert.assertNotNull(res.getBody());
			Assert.assertNotNull(res.getBody().getError());
			Assert.assertNotNull(res.getBody().getEvent());
			Assert.assertEquals(0, res.getBody().getEvent().size());
			Assert.assertEquals("Les critères de recherche sont vides.", res.getBody().getError().getErrorMessage());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, null, null, null);
			final ResponseEntity<SearchResult> res = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(res);
			Assert.assertNotNull(res.getBody());
			Assert.assertNotNull(res.getBody().getError());
			Assert.assertNotNull(res.getBody().getEvent());
			Assert.assertEquals(0, res.getBody().getEvent().size());
			Assert.assertEquals("Les critères de recherche sont vides.", res.getBody().getError().getErrorMessage());
		}
	}

	@Test
	public void testAvecResultats() throws Exception {
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, "031", null, null);
			final ResponseEntity<SearchResult> res = get(SearchResult.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(res);
			Assert.assertNotNull(res.getBody());
			Assert.assertNull(res.getBody().getError());
			Assert.assertNotNull(res.getBody().getEvent());
			Assert.assertTrue(res.getBody().getEvent().size() > 0);
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, "031", null, null);
			final ResponseEntity<SearchResult> res = get(SearchResult.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(res);
			Assert.assertNotNull(res.getBody());
			Assert.assertNull(res.getBody().getError());
			Assert.assertNotNull(res.getBody().getEvent());
			Assert.assertTrue(res.getBody().getEvent().size() > 0);
		}
	}
}
