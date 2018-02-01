package ch.vd.unireg.webservice.v7;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;

public class WebServiceModifiedTaxPayersItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceModifiedTaxPayersItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(@Nullable Date since, @Nullable Date until) {
		final Map<String, Object> map = new HashMap<>();
		final StringBuilder b = new StringBuilder();
		if (since != null) {
			map.put("since", since.getTime());
			b.append("&since={since}");
		}
		if (until != null) {
			map.put("until", until.getTime());
			b.append("&until={until}");
		}
		return Pair.<String, Map<String, ?>>of(String.format("/modifiedTaxPayers?user=zaizzt/22%s", b.toString()), map);
	}

	@Test
	public void testMissingParameter() throws Exception {
		// no parameter at all
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, null);
			final ResponseEntity<PartyNumberList> response = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}
		// only one
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2010, 1, 1).asJavaDate(), null);
			final ResponseEntity<PartyNumberList> response = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}
		// only the other
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(null, RegDate.get(2010, 1, 1).asJavaDate());
			final ResponseEntity<PartyNumberList> response = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}
		// both are there -> ok
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2010, 1, 1).asJavaDate(), RegDate.get(2010, 1, 2).asJavaDate());
			final ResponseEntity<PartyNumberList> response = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
		}
	}

	@Test
	public void testGetNone() throws Exception {
		// dans le fichier chargé, les dates de modifications sont au plus tard en 2009... rien depuis
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2010, 1, 1).asJavaDate(), RegDate.get(2011, 1, 1).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(0, reponse.getBody().getPartyNo().size());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2010, 1, 1).asJavaDate(), RegDate.get(2011, 1, 1).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(0, reponse.getBody().getPartyNo().size());
		}
	}

	@Test
	public void testGetOne() throws Exception {
		// dans le fichier chargé, les dates de modifications sont au plus tard en 2009... (et encore, une seule au premier janvier)
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2009, 1, 1).asJavaDate(), RegDate.get(2011, 1, 1).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(1, reponse.getBody().getPartyNo().size());

			final Integer partyNo = reponse.getBody().getPartyNo().get(0);
			Assert.assertEquals((Integer) 12100004, partyNo);
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2009, 1, 1).asJavaDate(), RegDate.get(2011, 1, 1).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(1, reponse.getBody().getPartyNo().size());

			final Integer partyNo = reponse.getBody().getPartyNo().get(0);
			Assert.assertEquals((Integer) 12100004, partyNo);
		}
	}

	@Test
	public void testGetTwo() throws Exception {
		// dans le fichier chargé, un tiers a été modifié le 1.1.2008, l'autre le 1.1.2009
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2008, 1, 1).asJavaDate(), RegDate.get(2009, 1, 2).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(2, reponse.getBody().getPartyNo().size());

			final List<Integer> sortedList = new ArrayList<>(reponse.getBody().getPartyNo());
			Collections.sort(sortedList);
			Assert.assertEquals((Integer) 12100003, sortedList.get(0));
			Assert.assertEquals((Integer) 12100004, sortedList.get(1));
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(RegDate.get(2008, 1, 1).asJavaDate(), RegDate.get(2009, 1, 2).asJavaDate());
			final ResponseEntity<PartyNumberList> reponse = get(PartyNumberList.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(reponse);
			Assert.assertNotNull(reponse.getBody());
			Assert.assertNotNull(reponse.getBody().getPartyNo());
			Assert.assertEquals(2, reponse.getBody().getPartyNo().size());

			final List<Integer> sortedList = new ArrayList<>(reponse.getBody().getPartyNo());
			Collections.sort(sortedList);
			Assert.assertEquals((Integer) 12100003, sortedList.get(0));
			Assert.assertEquals((Integer) 12100004, sortedList.get(1));
		}
	}
}
