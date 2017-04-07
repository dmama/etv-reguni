package ch.vd.uniregctb.xml.party.v5;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.AcquisitionReason;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LandRightBuilderTest {

	@Test
	public void testNewLandOwnershipRightPP() throws Exception {

		final Long ctbId = 2928282L;

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(8765887L);
		communaute.setType(TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
		communaute.setIdRF("a8283ee322");

		final MineRF immeuble = new MineRF();
		immeuble.setIdRF("a8388e8e83");
		immeuble.setId(123456L);

		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setRegime(GenrePropriete.COMMUNE);
		droit.setCommunaute(communaute);
		droit.setPart(new Fraction(2, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2017, 3, 2), "Succession", new IdentifiantAffaireRF(21, 2017, 17, 0)));
		droit.setAyantDroit(new PersonnePhysiqueRF());
		droit.setImmeuble(immeuble);
		droit.calculateDateEtMotifDebut();

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> ctbId);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(OwnershipType.COLLECTIVE_OWNERSHIP, landOwnershipRight.getType());
		assertShare(2, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landOwnershipRight.getDateTo()));
		assertEquals("Achat", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landOwnershipRight.getCaseIdentifier());
		assertEquals(Integer.valueOf(ctbId.intValue()), landOwnershipRight.getRightHolder().getTaxPayerNumber());
		assertEquals(123456L, landOwnershipRight.getImmovablePropertyId());
		assertEquals(Long.valueOf(8765887L), landOwnershipRight.getCommunityId());

		final List<AcquisitionReason> reasons = landOwnershipRight.getAcquisitionReasons();
		assertNotNull(reasons);
		assertEquals(2, reasons.size());
		assertAcquisitionReason(RegDate.get(2016, 9, 22), "Achat", 21, "2016/322/3", reasons.get(0));
		assertAcquisitionReason(RegDate.get(2017, 3, 2), "Succession", 21, "2017/17/0", reasons.get(1));
	}

	@Test
	public void testNewLandOwnershipRightPM() throws Exception {

		final Long ctbId = 2928282L;

		final DroitDistinctEtPermanentRF immeuble = new DroitDistinctEtPermanentRF();
		immeuble.setIdRF("a8388e8e83");
		immeuble.setId(123456L);

		final DroitProprietePersonneMoraleRF droit = new DroitProprietePersonneMoraleRF();
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setRegime(GenrePropriete.INDIVIDUELLE);
		droit.setCommunaute(null);
		droit.setPart(new Fraction(3, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(new PersonneMoraleRF());
		droit.setImmeuble(immeuble);
		droit.calculateDateEtMotifDebut();

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> ctbId);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(OwnershipType.SOLE_OWNERSHIP, landOwnershipRight.getType());
		assertShare(3, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landOwnershipRight.getDateTo()));
		assertEquals("Achat", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landOwnershipRight.getCaseIdentifier());
		assertEquals(Integer.valueOf(ctbId.intValue()), landOwnershipRight.getRightHolder().getTaxPayerNumber());
		assertEquals(123456L, landOwnershipRight.getImmovablePropertyId());
		assertNull(landOwnershipRight.getCommunityId());

		final List<AcquisitionReason> reasons = landOwnershipRight.getAcquisitionReasons();
		assertNotNull(reasons);
		assertEquals(1, reasons.size());
		assertAcquisitionReason(RegDate.get(2016, 9, 22), "Achat", 21, "2016/322/3", reasons.get(0));
	}

	@Test
	public void testNewLandOwnershipRightImmovableProperty() throws Exception {

		final Long dominantId = 2928282L;
		final long servantId = 4222L;

		final ProprieteParEtageRF dominant = new ProprieteParEtageRF();
		dominant.setIdRF("a8388e8e83");
		dominant.setId(dominantId);

		final ImmeubleBeneficiaireRF beneficiaire = new ImmeubleBeneficiaireRF();
		beneficiaire.setIdRF(dominant.getIdRF());
		beneficiaire.setImmeuble(dominant);

		final BienFondRF servant = new BienFondRF();
		servant.setIdRF("42432234");
		servant.setId(servantId);

		final DroitProprieteImmeubleRF droit = new DroitProprieteImmeubleRF();
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setRegime(GenrePropriete.FONDS_DOMINANT);
		droit.setPart(new Fraction(3, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Constitution de PPE", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(beneficiaire);
		droit.setImmeuble(servant);
		droit.calculateDateEtMotifDebut();

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> null);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(OwnershipType.DOMINANT_OWNERSHIP, landOwnershipRight.getType());
		assertShare(3, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landOwnershipRight.getDateTo()));
		assertEquals("Constitution de PPE", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landOwnershipRight.getCaseIdentifier());
		assertEquals(dominantId, landOwnershipRight.getRightHolder().getImmovablePropertyId());
		assertEquals(servantId, landOwnershipRight.getImmovablePropertyId());
		assertNull(landOwnershipRight.getCommunityId());

		final List<AcquisitionReason> reasons = landOwnershipRight.getAcquisitionReasons();
		assertNotNull(reasons);
		assertEquals(1, reasons.size());
		assertAcquisitionReason(RegDate.get(2016, 9, 22), "Constitution de PPE", 21, "2016/322/3", reasons.get(0));
	}

	@Test
	public void testNewUsufructRight() throws Exception {

		final long ctbId1 = 2928282L;
		final long ctbId2 = 4573282L;

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(ctbId1);

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(ctbId2);

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("a8388e8e83");
		immeuble1.setId(123456L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("a26271e8e2");
		immeuble2.setId(4783711L);
		immeuble2.setDroitsPropriete(Collections.emptySet());
		immeuble2.setServitudes(Collections.emptySet());

		final UsufruitRF usufruit = new UsufruitRF();
		usufruit.setDateDebut(RegDate.get(2016, 11, 3));
		usufruit.setDateFin(RegDate.get(2017, 9, 22));
		usufruit.setMotifDebut("Convention");
		usufruit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		usufruit.setDateFinMetier(RegDate.get(2017, 4, 14));
		usufruit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		usufruit.addAyantDroit(pp1);
		usufruit.addAyantDroit(pp2);
		usufruit.addImmeuble(immeuble1);
		usufruit.addImmeuble(immeuble2);

		final LandRight landRight = LandRightBuilder.newLandRight(usufruit, AyantDroitRF::getId);
		assertNotNull(landRight);
		assertTrue(landRight instanceof UsufructRight);

		final UsufructRight usufructRight = (UsufructRight) landRight;
		assertNotNull(usufructRight);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(usufructRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(usufructRight.getDateTo()));
		assertEquals("Convention", usufructRight.getStartReason());
		assertNull(usufructRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", usufructRight.getCaseIdentifier());

		final List<RightHolder> rightHolders = usufructRight.getRightHolders();
		assertEquals(2, rightHolders.size());
		assertEquals(Integer.valueOf((int) ctbId1), rightHolders.get(0).getTaxPayerNumber());
		assertEquals(Integer.valueOf((int) ctbId2), rightHolders.get(1).getTaxPayerNumber());

		final List<Long> immovablePropertyIds = usufructRight.getImmovablePropertyIds();
		assertEquals(2, immovablePropertyIds.size());
		assertEquals(Long.valueOf(123456L), immovablePropertyIds.get(0));
		assertEquals(Long.valueOf(4783711L), immovablePropertyIds.get(1));

		// pour des raisons de compatibilité ascendante, ces deux propriétés sont encore renseignées
		assertEquals(123456L, usufructRight.getImmovablePropertyId());
		assertEquals(Integer.valueOf((int) ctbId1), usufructRight.getRightHolder().getTaxPayerNumber());
	}

	@Test
	public void testNewHousingRight() throws Exception {

		final long ctbId1 = 2928282L;
		final long ctbId2 = 4573282L;

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(ctbId1);

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(ctbId2);

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("a8388e8e83");
		immeuble1.setId(123456L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("a26271e8e2");
		immeuble2.setId(4783711L);
		immeuble2.setDroitsPropriete(Collections.emptySet());
		immeuble2.setServitudes(Collections.emptySet());

		final DroitHabitationRF droitHabitation = new DroitHabitationRF();
		droitHabitation.setDateDebut(RegDate.get(2016, 11, 3));
		droitHabitation.setDateFin(RegDate.get(2017, 9, 22));
		droitHabitation.setMotifDebut("Convention");
		droitHabitation.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droitHabitation.setDateFinMetier(RegDate.get(2017, 4, 14));
		droitHabitation.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		droitHabitation.addAyantDroit(pp1);
		droitHabitation.addAyantDroit(pp2);
		droitHabitation.addImmeuble(immeuble1);
		droitHabitation.addImmeuble(immeuble2);

		final LandRight landRight = LandRightBuilder.newLandRight(droitHabitation, AyantDroitRF::getId);
		assertNotNull(landRight);
		assertTrue(landRight instanceof HousingRight);

		final HousingRight housingRight = (HousingRight) landRight;
		assertNotNull(housingRight);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(housingRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(housingRight.getDateTo()));
		assertEquals("Convention", housingRight.getStartReason());
		assertNull(housingRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", housingRight.getCaseIdentifier());

		final List<RightHolder> rightHolders = housingRight.getRightHolders();
		assertEquals(2, rightHolders.size());
		assertEquals(Integer.valueOf((int) ctbId1), rightHolders.get(0).getTaxPayerNumber());
		assertEquals(Integer.valueOf((int) ctbId2), rightHolders.get(1).getTaxPayerNumber());

		final List<Long> immovablePropertyIds = housingRight.getImmovablePropertyIds();
		assertEquals(2, immovablePropertyIds.size());
		assertEquals(Long.valueOf(123456L), immovablePropertyIds.get(0));
		assertEquals(Long.valueOf(4783711L), immovablePropertyIds.get(1));

		// pour des raisons de compatibilité ascendante, ces deux propriétés sont encore renseignées
		assertEquals(123456L, housingRight.getImmovablePropertyId());
		assertEquals(Integer.valueOf((int) ctbId1), housingRight.getRightHolder().getTaxPayerNumber());
	}

	private void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private static void assertCaseIdentifier(int officeNumber, String caseNumber, CaseIdentifier caseIdentifier) {
		assertNotNull(caseIdentifier);
		assertEquals(officeNumber, caseIdentifier.getOfficeNumber());
		assertEquals(caseNumber, caseIdentifier.getCaseNumberText());
	}

	private static void assertAcquisitionReason(RegDate date, String r, int officeNumber, String caseNumber, AcquisitionReason reason) {
		assertNotNull(reason);
		assertEquals(date, DataHelper.xmlToCore(reason.getDate()));
		assertEquals(r, reason.getReason());
		assertCaseIdentifier(officeNumber, caseNumber, reason.getCaseIdentifier());
	}
}