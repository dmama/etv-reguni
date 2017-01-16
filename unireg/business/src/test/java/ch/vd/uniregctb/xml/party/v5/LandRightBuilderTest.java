package ch.vd.uniregctb.xml.party.v5;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LandRightBuilderTest {

	@Test
	public void testNewLandOwnershipRightPP() throws Exception {

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(8765887L);
		communaute.setType(TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
		communaute.setIdRF("a8283ee322");

		final MineRF immeuble = new MineRF();
		immeuble.setIdRF("a8388e8e83");
		immeuble.setId(123456L);

		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setDateDebutOfficielle(RegDate.get(2016, 9, 22));
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setMotifDebut("Achat");
		droit.setRegime(GenrePropriete.COMMUNE);
		droit.setCommunaute(communaute);
		droit.setPart(new Fraction(2, 5));
		droit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		droit.setImmeuble(immeuble);

		final LandRight landRight = LandRightBuilder.newLandRight(droit);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight= (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landOwnershipRight.getType());
		assertShare(2, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertNull(landOwnershipRight.getDateTo());
		assertEquals("Achat", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, 2016, 322, 3, landOwnershipRight.getCaseIdentifier());
		assertEquals(123456L, landOwnershipRight.getImmovablePropertyId());
		assertCommunity(8765887L, CommunityOfOwnersType.COMMUNITY_OF_HEIRS, 666, Collections.singletonList(666), landOwnershipRight.getCommunity());
	}

	@Test
	public void testNewLandOwnershipRightPM() throws Exception {

		final DroitDistinctEtPermanentRF immeuble = new DroitDistinctEtPermanentRF();
		immeuble.setIdRF("a8388e8e83");
		immeuble.setId(123456L);

		final DroitProprietePersonneMoraleRF droit = new DroitProprietePersonneMoraleRF();
		droit.setDateDebutOfficielle(RegDate.get(2016, 9, 22));
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setMotifDebut("Achat");
		droit.setRegime(GenrePropriete.INDIVIDUELLE);
		droit.setCommunaute(null);
		droit.setPart(new Fraction(3, 5));
		droit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		droit.setImmeuble(immeuble);

		final LandRight landRight = LandRightBuilder.newLandRight(droit);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight= (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(OwnershipType.SOLE_OWNERSHIP, landOwnershipRight.getType());
		assertShare(3, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertNull(landOwnershipRight.getDateTo());
		assertEquals("Achat", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, 2016, 322, 3, landOwnershipRight.getCaseIdentifier());
		assertEquals(123456L, landOwnershipRight.getImmovablePropertyId());
		assertNull(landOwnershipRight.getCommunity());

	}

	private static void assertCommunity(long id, CommunityOfOwnersType type, int memberCount, List<Integer> memberIds, CommunityOfOwners community) {
		assertNotNull(community);
		assertEquals(id, community.getId());
		assertEquals(type, community.getType());
		assertEquals(memberCount, community.getMemberCount());
		assertEquals(memberIds, community.getMemberIds());
	}

	private void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private void assertCaseIdentifier(int officeNumber, Integer year, Integer number, Integer index, CaseIdentifier caseIdentifier) {
		assertNotNull(caseIdentifier);
		assertEquals(officeNumber, caseIdentifier.getOfficeNumber());
		assertEquals(year, caseIdentifier.getYear());
		assertEquals(number, caseIdentifier.getCaseNumber());
		assertEquals(index, caseIdentifier.getCaseIndex());
	}
}