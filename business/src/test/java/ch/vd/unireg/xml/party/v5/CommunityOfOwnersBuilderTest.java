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

	/**
	 * [IMM-1215] Ce test vérifie que le builder supporte de construire une communauté qui possède plusieurs droits propre (= un historique) sur un immeuble.
	 */
	@Test
	public void testNewCommunityAvecHistoriqueDesDroits() throws Exception {

		// les tiers RF et la communauté RF
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

		// la communauté possède 1/2 de l'immeuble
		final DroitProprieteCommunauteRF droit1 = new DroitProprieteCommunauteRF();
		droit1.setId(2332L);
		droit1.setMasterIdRF("28288228");
		droit1.setVersionIdRF("1");
		droit1.setDateDebut(RegDate.get(2016, 11, 3));
		droit1.setDateFin(RegDate.get(2017, 2, 1));
		droit1.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit1.setDateFinMetier(RegDate.get(2016, 12, 31));
		droit1.setMotifDebut("Succession");
		droit1.setMotifFin("Partage");
		droit1.setRegime(GenrePropriete.COMMUNE);
		droit1.setPart(new Fraction(1, 2));
		droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Succession", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit1.setAyantDroit(communaute);
		droit1.setImmeuble(immeuble);
		communaute.addDroitPropriete(droit1);

		// passage à 3/5 le 1er janvier 2017
		final DroitProprieteCommunauteRF droit2 = new DroitProprieteCommunauteRF();
		droit2.setId(2333L);
		droit2.setMasterIdRF("28288228");
		droit2.setVersionIdRF("1");
		droit2.setDateDebut(RegDate.get(2017, 2, 2));
		droit2.setDateFin(RegDate.get(2017, 9, 22));
		droit2.setDateDebutMetier(RegDate.get(2017, 1, 1));
		droit2.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit2.setMotifDebut("Partage");
		droit2.setRegime(GenrePropriete.COMMUNE);
		droit2.setPart(new Fraction(3, 5));
		droit2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Succession", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2017, 1, 1), "Partage", new IdentifiantAffaireRF(21, 2016, 578, 1)));
		droit2.setAyantDroit(communaute);
		droit2.setImmeuble(immeuble);
		communaute.addDroitPropriete(droit2);

		// les informations sur les membres
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

		// [IMM-1215] on expose les deux droits
		final List<LandOwnershipRight> landRights = community.getLandRights();
		assertNotNull(landRights);
		assertEquals(2, landRights.size());

		final LandOwnershipRight landRight0 = landRights.get(0);
		assertNotNull(landRight0);
		assertEquals(2332L, landRight0.getId());
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landRight0.getType());
		assertShare(1, 2, landRight0.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landRight0.getDateFrom()));
		assertEquals(RegDate.get(2016, 12, 31), DataHelper.xmlToCore(landRight0.getDateTo()));
		assertEquals("Succession", landRight0.getStartReason());
		assertEquals("Partage", landRight0.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landRight0.getCaseIdentifier());
		assertEquals(Long.valueOf(234342L), landRight0.getRightHolder().getCommunityId());
		assertEquals(123456L, landRight0.getImmovablePropertyId());
		assertNull(landRight0.getCommunityId());

		final LandOwnershipRight landRight1 = landRights.get(1);
		assertNotNull(landRight1);
		assertEquals(2333L, landRight1.getId());
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landRight1.getType());
		assertShare(3, 5, landRight1.getShare());
		assertEquals(RegDate.get(2017, 1, 1), DataHelper.xmlToCore(landRight1.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landRight1.getDateTo()));
		assertEquals("Partage", landRight1.getStartReason());
		assertNull(landRight1.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landRight1.getCaseIdentifier());
		assertEquals(Long.valueOf(234342L), landRight1.getRightHolder().getCommunityId());
		assertEquals(123456L, landRight1.getImmovablePropertyId());
		assertNull(landRight1.getCommunityId());

		// [IMM-1215] on expose le dernier droit (pour être compatible ascendant)
		final LandOwnershipRight landRight = community.getLandRight();
		assertNotNull(landRight);
		assertEquals(2333L, landRight.getId());
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landRight.getType());
		assertShare(3, 5, landRight.getShare());
		assertEquals(RegDate.get(2017, 1, 1), DataHelper.xmlToCore(landRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landRight.getDateTo()));
		assertEquals("Partage", landRight.getStartReason());
		assertNull(landRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landRight.getCaseIdentifier());
		assertEquals(Long.valueOf(234342L), landRight.getRightHolder().getCommunityId());
		assertEquals(123456L, landRight.getImmovablePropertyId());
		assertNull(landRight.getCommunityId());
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