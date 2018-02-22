package ch.vd.unireg.xml.party.v5;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommunauteRFAppartenanceInfo;
import ch.vd.unireg.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.unireg.registrefoncier.CommunauteRFPrincipalInfo;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityLeader;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnerMembership;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;

import static ch.vd.unireg.xml.party.v5.LandRightBuilderTest.assertCaseIdentifier;
import static ch.vd.unireg.xml.party.v5.LandRightBuilderTest.assertShare;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CommunityOfOwnersBuilderTest {
	@Test
	public void testNewCommunity() throws Exception {

		final PersonnePhysiqueRF ppRF = new PersonnePhysiqueRF();
		ppRF.setDateNaissance(RegDate.get(1922,3,23));
		ppRF.setPrenom("Arnold");
		ppRF.setNom("Whitenegger");

		final PersonneMoraleRF pmRF = new PersonneMoraleRF();
		pmRF.setRaisonSociale("Ma petite entreprise");
		pmRF.setNumeroRC("CH3823838228");

		final CollectivitePubliqueRF collRF = new CollectivitePubliqueRF();
		collRF.setRaisonSociale("Mon petit club de foot");

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(234342L);
		communaute.setIdRF("388289282");
		communaute.setType(TypeCommunaute.INDIVISION);

		final DroitDistinctEtPermanentRF immeuble = new DroitDistinctEtPermanentRF();
		immeuble.setIdRF("a8388e8e83");
		immeuble.setId(123456L);

		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setId(2332L);
		droit.setMasterIdRF("28288228");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setMotifDebut("Succession");
		droit.setRegime(GenrePropriete.COMMUNE);
		droit.setPart(new Fraction(1, 1));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Succession", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(communaute);
		droit.setImmeuble(immeuble);
		communaute.addDroitPropriete(droit);

		CommunauteRFAppartenanceInfo appartenance1 = new CommunauteRFAppartenanceInfo(RegDate.get(2016, 9, 22), RegDate.get(2017, 4, 14), null, null, 2727272L);
		CommunauteRFAppartenanceInfo appartenance2 = new CommunauteRFAppartenanceInfo(RegDate.get(2016, 9, 22), RegDate.get(2004, 8, 29), null, ppRF, null);

		final CommunauteRFMembreInfo membreInfo = new CommunauteRFMembreInfo(Collections.singletonList(2727272L),
		                                                                     Arrays.asList(ppRF, pmRF, collRF),
		                                                                     Arrays.asList(appartenance1, appartenance2));
		membreInfo.setPrincipaux(Collections.singletonList(new CommunauteRFPrincipalInfo(null,
		                                                                                 null, RegDate.get(2016, 9, 22), RegDate.get(2017, 4, 14), null, 2727272L, false)));

		final CommunityOfOwners community = CommunityOfOwnersBuilder.newCommunity(communaute, id -> null, id -> membreInfo);
		assertNotNull(community);
		assertEquals(234342L, community.getId());
		assertEquals(CommunityOfOwnersType.JOINT_OWNERSHIP, community.getType());

		final List<RightHolder> members = community.getMembers();
		assertEquals(4, members.size());
		assertRightHolderParty(2727272L, members.get(0));
		assertRightHolderNaturalPerson("Arnold", "Whitenegger", RegDate.get(1922,3,23), members.get(1));
		assertRightHolderCorporation("Ma petite entreprise", "CH3823838228", members.get(2));
		assertRightHolderAdministrativeAuthority("Mon petit club de foot", members.get(3));

		final LandOwnershipRight landRight = community.getLandRight();
		assertNotNull(landRight);
		assertEquals(2332L, landRight.getId());
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landRight.getType());
		assertShare(1, 1, landRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landRight.getDateTo()));
		assertEquals("Succession", landRight.getStartReason());
		assertNull(landRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landRight.getCaseIdentifier());
		assertEquals(Long.valueOf(234342L), landRight.getRightHolder().getCommunityId());
		assertEquals(123456L, landRight.getImmovablePropertyId());
		assertNull(landRight.getCommunityId());

		final List<CommunityLeader> leaders = community.getLeaders();
		assertNotNull(leaders);
		assertEquals(1, leaders.size());
		final CommunityLeader leader0 = leaders.get(0);
		assertNotNull(leader0);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(leader0.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(leader0.getDateTo()));
		assertEquals(2727272, leader0.getTaxPayerNumber());

		final List<CommunityOfOwnerMembership> memberships = community.getMemberships();
		assertNotNull(memberships);
		assertEquals(2, memberships.size());

		final CommunityOfOwnerMembership membership0 = memberships.get(0);
		assertNotNull(membership0);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(membership0.getDateFrom()));
		assertEquals(RegDate.get(2004, 8, 29), DataHelper.xmlToCore(membership0.getDateTo()));
		assertNull(membership0.getCancellationDate());
		assertRightHolderNaturalPerson("Arnold", "Whitenegger", RegDate.get(1922,3,23), membership0.getRightHolder());

		final CommunityOfOwnerMembership membership1 = memberships.get(1);
		assertNotNull(membership1);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(membership1.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(membership1.getDateTo()));
		assertNull(membership1.getCancellationDate());
		assertRightHolderParty(2727272, membership1.getRightHolder());
	}

	private static void assertRightHolderNaturalPerson(String firstName, String lastName, RegDate dateOfBirth, RightHolder rightHolder) {
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) rightHolder.getIdentity();
		Assert.assertEquals(firstName, identity.getFirstName());
		Assert.assertEquals(lastName, identity.getLastName());
		Assert.assertEquals(dateOfBirth, DataHelper.xmlToCore(identity.getDateOfBirth()));
	}

	private static void assertRightHolderCorporation(String name, String commercialRegisterNumber, RightHolder rightHolder) {
		final CorporationIdentity identity = (CorporationIdentity) rightHolder.getIdentity();
		Assert.assertEquals(name, identity.getName());
		Assert.assertEquals(commercialRegisterNumber, identity.getCommercialRegisterNumber());
	}

	private static void assertRightHolderAdministrativeAuthority(String name, RightHolder rightHolder) {
		final AdministrativeAuthorityIdentity identity = (AdministrativeAuthorityIdentity) rightHolder.getIdentity();
		Assert.assertEquals(name, identity.getName());
	}

	private static void assertRightHolderParty(long id, RightHolder rightHolder) {
		Assert.assertEquals(Integer.valueOf((int) id), rightHolder.getTaxPayerNumber());
	}
}