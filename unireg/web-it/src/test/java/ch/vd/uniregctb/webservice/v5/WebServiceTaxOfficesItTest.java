package ch.vd.uniregctb.webservice.v5;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;

public class WebServiceTaxOfficesItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceTaxOfficesItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(int noOfsCommune, @Nullable RegDate date) {
		final Map<String, Object> map = new HashMap<>();
		map.put("municipalityId", noOfsCommune);
		if (date != null) {
			map.put("date", RegDateHelper.dateToDisplayString(date));
			return Pair.<String, Map<String, ?>>of("/taxOffices/{municipalityId}?date={date}", map);
		}
		else {
			return Pair.<String, Map<String, ?>>of("/taxOffices/{municipalityId}", map);
		}
	}

	@Test
	public void testCommuneVaudoise() throws Exception {

		// commune de Château d'Oex

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(5841, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final TaxOffices taxOffices = resp.getBody();
			Assert.assertNotNull(taxOffices);
			Assert.assertNotNull(taxOffices.getDistrict());
			Assert.assertNotNull(taxOffices.getRegion());
			Assert.assertEquals(2000015, taxOffices.getDistrict().getPartyNo());
			Assert.assertEquals(16, taxOffices.getDistrict().getAdmCollNo());
			Assert.assertEquals(2000017, taxOffices.getRegion().getPartyNo());
			Assert.assertEquals(18, taxOffices.getRegion().getAdmCollNo());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(5841, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final TaxOffices taxOffices = resp.getBody();
			Assert.assertNotNull(taxOffices);
			Assert.assertNotNull(taxOffices.getDistrict());
			Assert.assertNotNull(taxOffices.getRegion());
			Assert.assertEquals(2000015, taxOffices.getDistrict().getPartyNo());
			Assert.assertEquals(16, taxOffices.getDistrict().getAdmCollNo());
			Assert.assertEquals(2000017, taxOffices.getRegion().getPartyNo());
			Assert.assertEquals(18, taxOffices.getRegion().getAdmCollNo());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(5841, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final TaxOffices taxOffices = resp.getBody();
			Assert.assertNotNull(taxOffices);
			Assert.assertNotNull(taxOffices.getDistrict());
			Assert.assertNotNull(taxOffices.getRegion());
			Assert.assertEquals(2000015, taxOffices.getDistrict().getPartyNo());
			Assert.assertEquals(16, taxOffices.getDistrict().getAdmCollNo());
			Assert.assertEquals(2000017, taxOffices.getRegion().getPartyNo());
			Assert.assertEquals(18, taxOffices.getRegion().getAdmCollNo());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(5841, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final TaxOffices taxOffices = resp.getBody();
			Assert.assertNotNull(taxOffices);
			Assert.assertNotNull(taxOffices.getDistrict());
			Assert.assertNotNull(taxOffices.getRegion());
			Assert.assertEquals(2000015, taxOffices.getDistrict().getPartyNo());
			Assert.assertEquals(16, taxOffices.getDistrict().getAdmCollNo());
			Assert.assertEquals(2000017, taxOffices.getRegion().getPartyNo());
			Assert.assertEquals(18, taxOffices.getRegion().getAdmCollNo());
		}
	}

	@Test
	public void testCommuneHorsCanton() throws Exception {

		// commune de Bâle

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(2701, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(2701, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(2701, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(2701, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
	}

	@Test
	public void testCommuneInconnue() throws Exception {

		// commune ???

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(9999, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(9999, null);
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(9999, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(9999, RegDate.get());
			final ResponseEntity<TaxOffices> resp = get(TaxOffices.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		}
	}
}
