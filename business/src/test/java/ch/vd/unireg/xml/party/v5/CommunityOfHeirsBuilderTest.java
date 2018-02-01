package ch.vd.unireg.xml.party.v5;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirLeader;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirMember;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CommunityOfHeirsBuilderTest {

	/**
	 * Ce test vérifie que la construction d'une communauté se fait bien dans le cas simple.
	 */
	@Test
	public void testNewCommunity() throws Exception {

		final PersonnePhysique pp = new PersonnePhysique();
		pp.setNumero(123456L);
		final Set<RapportEntreTiers> rapports = new HashSet<>();

		// deux héritiers
		rapports.add(new Heritage(RegDate.get(2016, 4, 1), null, 333L, 123456L, true));
		rapports.add(new Heritage(RegDate.get(2016, 4, 6), null, 444L, 123456L, false));
		pp.setRapportsObjet(rapports);

		final CommunityOfHeirs community = CommunityOfHeirsBuilder.newCommunity(pp);
		assertNotNull(community);
		assertEquals(123456, community.getInheritedFromNumber());
		assertEquals(RegDate.get(2016, 4, 1), DataHelper.xmlToCore(community.getInheritanceDateFrom()));

		final List<CommunityOfHeirMember> members = community.getMembers();
		assertNotNull(members);
		assertEquals(2, members.size());
		assertMember(333, RegDate.get(2016, 4, 1), null, null, members.get(0));
		assertMember(444, RegDate.get(2016, 4, 6), null, null, members.get(1));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		assertNotNull(leaders);
		assertEquals(1, leaders.size());
		assertLeader(333, RegDate.get(2016, 4, 1), null, null, leaders.get(0));
	}

	/**
	 * Ce test vérifie que la construction d'une communauté retourne un objet nul lorsqu'il n'y a aucun héritier
	 */
	@Test
	public void testNewCommunityAucunHeritier() throws Exception {

		// pas d'héritier
		final PersonnePhysique pp = new PersonnePhysique();
		pp.setNumero(123456L);
		pp.setRapportsObjet(new HashSet<>());
		assertNull(CommunityOfHeirsBuilder.newCommunity(pp));
	}

	/**
	 * Ce test vérifie que la construction d'une communauté retourne un objet nul lorsqu'il n'y a qu'un seul héritier
	 */
	@Test
	public void testNewCommunityUnSeulHeritier() throws Exception {

		final PersonnePhysique pp = new PersonnePhysique();
		pp.setNumero(123456L);
		final Set<RapportEntreTiers> rapports = new HashSet<>();

		// un seul héritier
		rapports.add(new Heritage(RegDate.get(2016, 4, 1), null, 333L, 123456L, true));
		pp.setRapportsObjet(rapports);

		assertNull(CommunityOfHeirsBuilder.newCommunity(pp));
	}

	/**
	 * Ce test vérifie que la construction d'une communauté se fait bien dans le cas où le leader de communauté change d'un héritier à l'autre.
	 */
	@Test
	public void testNewCommunityChangementPrincipal() throws Exception {

		final PersonnePhysique pp = new PersonnePhysique();
		pp.setNumero(123456L);
		final Set<RapportEntreTiers> rapports = new HashSet<>();

		// deux héritiers : le premier était le principal jusqu'au 31.12.2016, puis le second est le principal à partir du 01.01.2017
		rapports.add(new Heritage(RegDate.get(2016, 4, 1), RegDate.get(2016, 12, 31), 333L, 123456L, true));
		rapports.add(new Heritage(RegDate.get(2017, 1, 1), null, 333L, 123456L, false));
		rapports.add(new Heritage(RegDate.get(2016, 4, 6), RegDate.get(2016, 12, 31), 444L, 123456L, false));
		rapports.add(new Heritage(RegDate.get(2017, 1, 1), null, 444L, 123456L, true));
		pp.setRapportsObjet(rapports);

		final CommunityOfHeirs community = CommunityOfHeirsBuilder.newCommunity(pp);
		assertNotNull(community);
		assertEquals(123456, community.getInheritedFromNumber());
		assertEquals(RegDate.get(2016, 4, 1), DataHelper.xmlToCore(community.getInheritanceDateFrom()));

		final List<CommunityOfHeirMember> members = community.getMembers();
		assertNotNull(members);
		assertEquals(2, members.size());
		assertMember(333, RegDate.get(2016, 4, 1), null, null, members.get(0));
		assertMember(444, RegDate.get(2016, 4, 6), null, null, members.get(1));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		assertNotNull(leaders);
		assertEquals(2, leaders.size());
		assertLeader(333, RegDate.get(2016, 4, 1), RegDate.get(2016, 12, 31), null, leaders.get(0));
		assertLeader(444, RegDate.get(2017, 1, 1), null, null, leaders.get(1));
	}

	/**
	 * Ce test vérifie que la construction d'une communauté se fait bien dans le cas où le leader de communauté change
	 * d'un héritier à l'autre <i>et</i> que des liens d'héritage sont annulés.
	 */
	@Test
	public void testNewCommunityChangementPrincipalAvecLiensAnnules() throws Exception {

		final PersonnePhysique pp = new PersonnePhysique();
		pp.setNumero(123456L);
		final Set<RapportEntreTiers> rapports = new HashSet<>();

		// deux héritiers : le premier était le principal jusqu'au 31.12.2016, puis le second est le principal à partir du 01.01.2017
		// + deux liens d'héritage annulés
		final Heritage h1 = new Heritage(RegDate.get(2016, 4, 1), null, 333L, 123456L, true);
		h1.setAnnulationDate(DateHelper.getDate(2016, 11, 23));
		rapports.add(h1);
		rapports.add(new Heritage(RegDate.get(2016, 4, 1), RegDate.get(2016, 12, 31), 333L, 123456L, true));
		rapports.add(new Heritage(RegDate.get(2017, 1, 1), null, 333L, 123456L, false));
		final Heritage h2 = new Heritage(RegDate.get(2016, 4, 6), null, 444L, 123456L, false);
		h2.setAnnulationDate(DateHelper.getDate(2016, 11, 23));
		rapports.add(h2);
		rapports.add(new Heritage(RegDate.get(2016, 4, 6), RegDate.get(2016, 12, 31), 444L, 123456L, false));
		rapports.add(new Heritage(RegDate.get(2017, 1, 1), null, 444L, 123456L, true));
		pp.setRapportsObjet(rapports);

		final CommunityOfHeirs community = CommunityOfHeirsBuilder.newCommunity(pp);
		assertNotNull(community);
		assertEquals(123456, community.getInheritedFromNumber());
		assertEquals(RegDate.get(2016, 4, 1), DataHelper.xmlToCore(community.getInheritanceDateFrom()));

		final List<CommunityOfHeirMember> members = community.getMembers();
		assertNotNull(members);
		assertEquals(4, members.size());
		assertMember(333, RegDate.get(2016, 4, 1), null, null, members.get(0));
		assertMember(444, RegDate.get(2016, 4, 6), null, null, members.get(1));
		assertMember(333, RegDate.get(2016, 4, 1), null, RegDate.get(2016, 11, 23), members.get(2));
		assertMember(444, RegDate.get(2016, 4, 6), null, RegDate.get(2016, 11, 23), members.get(3));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		assertNotNull(leaders);
		assertEquals(3, leaders.size());
		assertLeader(333, RegDate.get(2016, 4, 1), RegDate.get(2016, 12, 31), null, leaders.get(0));
		assertLeader(444, RegDate.get(2017, 1, 1), null, null, leaders.get(1));
		assertLeader(333, RegDate.get(2016, 4, 1), null, RegDate.get(2016, 11, 23), leaders.get(2));
	}

	private static void assertMember(int taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirMember member) {
		assertEquals(taxPayerNumber, member.getTaxPayerNumber());
		assertEquals(dateFrom, DataHelper.xmlToCore(member.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(member.getDateTo()));
		assertEquals(cancellationDate, DataHelper.xmlToCore(member.getCancellationDate()));
	}

	private static void assertLeader(int taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirLeader leader) {
		assertEquals(taxPayerNumber, leader.getTaxPayerNumber());
		assertEquals(dateFrom, DataHelper.xmlToCore(leader.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(leader.getDateTo()));
		assertEquals(cancellationDate, DataHelper.xmlToCore(leader.getCancellationDate()));
	}
}