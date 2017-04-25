package ch.vd.uniregctb.xml.party.v5;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.xml.DataHelper;

import static ch.vd.uniregctb.xml.party.v5.LandRightBuilderTest.assertCaseIdentifier;
import static ch.vd.uniregctb.xml.party.v5.LandRightBuilderTest.assertShare;
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
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setRegime(GenrePropriete.COMMUNE);
		droit.setPart(new Fraction(1, 1));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Succession", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(communaute);
		droit.setImmeuble(immeuble);
		droit.calculateDateEtMotifDebut();
		communaute.addDroitPropriete(droit);

		final CommunauteRFMembreInfo membreInfo = new CommunauteRFMembreInfo(4, Collections.singletonList(2727272L), Arrays.asList(ppRF, pmRF, collRF));

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