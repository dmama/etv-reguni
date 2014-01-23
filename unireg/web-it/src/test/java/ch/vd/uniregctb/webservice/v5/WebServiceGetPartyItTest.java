package ch.vd.uniregctb.webservice.v5;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.xml.party.person.v3.CommonHousehold;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;

public class WebServiceGetPartyItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceGetPartyItTest.xml";

	private static boolean alreadySetUp = false;

	private static abstract class PartyJsonContainer<T extends Party> {
		private T data;
		private String dataType;

		@JsonProperty(value = "data")
		public T getData() {
			return data;
		}

		@JsonProperty(value = "type")
		public String getDataType() {
			return dataType;
		}
	}

	private static final class CommonHouseholdJsonContainer extends PartyJsonContainer<CommonHousehold> {}
	private static final class NaturalPersonJsonContainer extends PartyJsonContainer<NaturalPerson> {}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(int partyNo, @Nullable Set<PartyPart> parts) {
		final Map<String, Object> map = new HashMap<>();
		map.put("partyNo", partyNo);

		final StringBuilder sbParts = new StringBuilder();
		if (parts != null) {
			for (PartyPart part : parts) {
				sbParts.append("&part=").append(part);
			}
		}
		return Pair.<String, Map<String, ?>>of("/party/{partyNo}?user=zaizzt/22" + sbParts.toString(), map);
	}

	@Test
	public void testWithoutPart() throws Exception {
		final int noTiers = 10035633;

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, null);
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());
			Assert.assertEquals("De Wit Tummers Elisabeth", ((NaturalPerson) party).getOfficialName());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, null);
			final ResponseEntity<NaturalPersonJsonContainer> resp = get(NaturalPersonJsonContainer.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final NaturalPersonJsonContainer partyContainer = resp.getBody();
			Assert.assertNotNull(partyContainer);
			Assert.assertEquals("NATURAL_PERSON", partyContainer.getDataType());
			Assert.assertEquals("De Wit Tummers Elisabeth", partyContainer.getData().getOfficialName());
		}
	}

	@Test
	public void testWithPart() throws Exception {
		final int noTiers = 10711803;
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.BANK_ACCOUNTS));
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());
			Assert.assertEquals("Cédric Allora", ((NaturalPerson) party).getOfficialName());
			Assert.assertEquals("CH7400243243G15379860", party.getBankAccounts().get(0).getAccountNumber());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.BANK_ACCOUNTS));
			final ResponseEntity<NaturalPersonJsonContainer> resp = get(NaturalPersonJsonContainer.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final NaturalPersonJsonContainer partyContainer = resp.getBody();
			Assert.assertNotNull(partyContainer);
			Assert.assertEquals("NATURAL_PERSON", partyContainer.getDataType());
			Assert.assertEquals("Cédric Allora", partyContainer.getData().getOfficialName());
			Assert.assertEquals("CH7400243243G15379860", partyContainer.getData().getBankAccounts().get(0).getAccountNumber());
		}
	}
}
