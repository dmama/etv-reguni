package ch.vd.unireg.webservice.v7;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.xml.party.adminauth.v5.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.debtor.v5.Debtor;
import ch.vd.unireg.xml.party.establishment.v2.Establishment;
import ch.vd.unireg.xml.party.othercomm.v3.OtherCommunity;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;

@SuppressWarnings("Duplicates")
public class WebServiceGetPartyItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceGetPartyItTest.xml";

	private static boolean alreadySetUp = false;

	private static final class PartyJsonContainer {
		public NaturalPerson naturalPerson;
		public CommonHousehold commonHousehold;
		public Debtor debtor;
		public Corporation corporation;
		public AdministrativeAuthority administrativeAuthority;
		public OtherCommunity otherCommunity;
		public Establishment establishment;
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	public static Pair<String, Map<String, ?>> buildUriAndParams(int partyNo, @Nullable Set<PartyPart> parts) {
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
			Assert.assertEquals("Tummers-De Wit Wouter", ((NaturalPerson) party).getOfficialName());
			Assert.assertEquals("Elisabeth", ((NaturalPerson) party).getFirstName());
			Assert.assertEquals("Elisabeth Astrid Mary", ((NaturalPerson) party).getFirstNames());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, null);
			final ResponseEntity<PartyJsonContainer> resp = get(PartyJsonContainer.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final PartyJsonContainer partyContainer = resp.getBody();
			Assert.assertNotNull(partyContainer);
			Assert.assertNull(partyContainer.administrativeAuthority);
			Assert.assertNull(partyContainer.commonHousehold);
			Assert.assertNull(partyContainer.corporation);
			Assert.assertNull(partyContainer.debtor);
			Assert.assertNotNull(partyContainer.naturalPerson);
			Assert.assertNull(partyContainer.otherCommunity);
			Assert.assertNull(partyContainer.establishment);

			Assert.assertEquals("Tummers-De Wit Wouter", partyContainer.naturalPerson.getOfficialName());
			Assert.assertEquals("Elisabeth", partyContainer.naturalPerson.getFirstName());
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
			Assert.assertEquals("Allora", ((NaturalPerson) party).getOfficialName());
			Assert.assertEquals("Cédric", ((NaturalPerson) party).getFirstName());
			Assert.assertEquals("CH7400243243G15379860", party.getBankAccounts().get(0).getIban());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.BANK_ACCOUNTS));
			final ResponseEntity<PartyJsonContainer> resp = get(PartyJsonContainer.class, MediaType.APPLICATION_JSON, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final PartyJsonContainer partyContainer = resp.getBody();
			Assert.assertNotNull(partyContainer);
			Assert.assertNull(partyContainer.administrativeAuthority);
			Assert.assertNull(partyContainer.commonHousehold);
			Assert.assertNull(partyContainer.corporation);
			Assert.assertNull(partyContainer.debtor);
			Assert.assertNotNull(partyContainer.naturalPerson);
			Assert.assertNull(partyContainer.otherCommunity);
			Assert.assertNull(partyContainer.establishment);

			Assert.assertEquals("Allora", partyContainer.naturalPerson.getOfficialName());
			Assert.assertEquals("Cédric", partyContainer.naturalPerson.getFirstName());
			Assert.assertEquals("CH7400243243G15379860", partyContainer.naturalPerson.getBankAccounts().get(0).getIban());
		}
	}

	@Test
	public void testDebiteurPrestationImposableEtFlagAciAutreCanton() throws Exception {
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(12500001, null);
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			Assert.assertNotNull(party);
			Assert.assertEquals(Debtor.class, party.getClass());
			Assert.assertFalse(((Debtor) party).isOtherCantonTaxAdministration());
		}
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(12500002, null);
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			Assert.assertNotNull(party);
			Assert.assertEquals(Debtor.class, party.getClass());
			Assert.assertTrue(((Debtor) party).isOtherCantonTaxAdministration());
		}
	}
}
