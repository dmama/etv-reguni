package ch.vd.unireg.webservice.v7;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirLeader;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirMember;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;

import static ch.vd.unireg.webservice.v7.WebServiceLandRegistryItTest.assertDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("Duplicates")
public class WebServiceGetCommunityOfHeirsItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceGetCommunityOfHeirsItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	@Test
	public void testGetCommunityOfHeirs() throws Exception {

		final int deceasedId = 10035633;
		final int heritier1Id = 10092818;   // De Wit Tummers Gertrude
		final int heritier2Id = 10092819;   // De Wit Tummers Aglaë

		final ResponseEntity<CommunityOfHeirs> resp = get(CommunityOfHeirs.class, MediaType.APPLICATION_XML, "/communityOfHeirs/{deceasedId}?user=zaizzt/22", Collections.singletonMap("deceasedId", deceasedId));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final CommunityOfHeirs community = resp.getBody();
		assertNotNull(community);
		Assert.assertEquals(deceasedId, community.getInheritedFromNumber());
		assertDate(RegDate.get(2017, 11, 1), community.getInheritanceDateFrom());

		final List<CommunityOfHeirMember> members = community.getMembers();
		Assert.assertNotNull(members);
		Assert.assertEquals(2, members.size());
		assertMember(heritier1Id, RegDate.get(2017, 11, 1), null, null, members.get(0));
		assertMember(heritier2Id, RegDate.get(2017, 11, 15), null, null, members.get(1));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		Assert.assertNotNull(leaders);
		Assert.assertEquals(1, leaders.size());
		assertLeader(heritier1Id, RegDate.get(2017, 11, 1), null, null, leaders.get(0));
	}

	private static void assertMember(long taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirMember member) {
		assertEquals(taxPayerNumber, member.getTaxPayerNumber());
		assertDate(dateFrom, member.getDateFrom());
		assertDate(dateTo, member.getDateTo());
		assertDate(cancellationDate, member.getCancellationDate());
	}

	private static void assertLeader(long taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirLeader leader) {
		assertEquals(taxPayerNumber, leader.getTaxPayerNumber());
		assertDate(dateFrom, leader.getDateFrom());
		assertDate(dateTo, leader.getDateTo());
		assertDate(cancellationDate, leader.getCancellationDate());
	}
}
