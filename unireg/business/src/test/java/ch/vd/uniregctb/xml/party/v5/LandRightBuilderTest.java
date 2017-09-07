package ch.vd.uniregctb.xml.party.v5;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.xml.party.landregistry.v1.AcquisitionReason;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualUsufructRight;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteVirtuelRF;
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
import ch.vd.uniregctb.registrefoncier.UsufruitVirtuelRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LandRightBuilderTest {

	private EasementRightHolderComparator rightHolderComparator;
	private Map<Long, Tiers> tiersMap;
	private Map<Long, List<ForFiscalPrincipal>> forsVirtuels;
	private Map<Long, NomPrenom> nomPrenomMap;
	private Map<Long, String> raisonSocialeMap;

	@Before
	public void setUp() throws Exception {

		this.tiersMap = new HashMap<>();
		this.forsVirtuels = new HashMap<>();
		this.nomPrenomMap = new HashMap<>();
		this.raisonSocialeMap = new HashMap<>();

		final Function<Long, Tiers> tiersGetter = tiersMap::get;
		final Function<Tiers, List<ForFiscalPrincipal>> forsVirtuelsGetter = tiers -> forsVirtuels.get(tiers.getNumero());
		final Function<PersonnePhysique, NomPrenom> nomPrenomGetter = pp -> nomPrenomMap.get(pp.getNumero());
		final Function<Tiers, String> raisonSocialeGetter = tiers -> raisonSocialeMap.get(tiers.getNumero());

		rightHolderComparator = new EasementRightHolderComparator(tiersGetter, forsVirtuelsGetter, nomPrenomGetter, raisonSocialeGetter);
	}

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
		droit.setId(2332L);
		droit.setMasterIdRF("28288228");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setMotifDebut("Achat");
		droit.setRegime(GenrePropriete.COMMUNE);
		droit.setCommunaute(communaute);
		droit.setPart(new Fraction(2, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2017, 3, 2), "Succession", new IdentifiantAffaireRF(21, 2017, 17, 0)));
		droit.setAyantDroit(new PersonnePhysiqueRF());
		droit.setImmeuble(immeuble);

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> ctbId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(2332L, landOwnershipRight.getId());
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
		droit.setId(2332L);
		droit.setMasterIdRF("28288228");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setMotifDebut("Achat");
		droit.setRegime(GenrePropriete.INDIVIDUELLE);
		droit.setCommunaute(null);
		droit.setPart(new Fraction(3, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(new PersonneMoraleRF());
		droit.setImmeuble(immeuble);

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> ctbId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(2332L, landOwnershipRight.getId());
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

		final BienFondsRF servant = new BienFondsRF();
		servant.setIdRF("42432234");
		servant.setId(servantId);

		final DroitProprieteImmeubleRF droit = new DroitProprieteImmeubleRF();
		droit.setId(2332L);
		droit.setMasterIdRF("28288228");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setMotifDebut("Constitution de PPE");
		droit.setRegime(GenrePropriete.FONDS_DOMINANT);
		droit.setPart(new Fraction(3, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Constitution de PPE", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(beneficiaire);
		droit.setImmeuble(servant);

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> null, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(2332L, landOwnershipRight.getId());
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

	/**
	 * <pre>
	 *                        individuelle (1/1)              +------------+
	 *                     +--------------------------------->| Immeuble 0 |
	 *     +----------+    |                                  +------------+
	 *     |          |----+                                     |
	 *     | Tiers RF |                                          | fond dominant (20/100)
	 *     |          |....+                                     v
	 *     +----------+    :  droit virtuel (1/1 * 20/100)    +------------+
	 *                     +.................................>| Immeuble 1 |
	 *                                                        +------------+
	 * </pre>
	 */
	@Test
	public void testNewLandOwnershipRightVirtualLandRight() throws Exception {

		final Long ctbId = 83838822L;

		final Long ppId = 8292L;
		final long dominantId = 2928282L;
		final long servantId = 4222L;

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("03040303");
		pp.setId(ppId);

		final ProprieteParEtageRF immeuble0 = new ProprieteParEtageRF();
		immeuble0.setIdRF("a8388e8e83");
		immeuble0.setId(dominantId);

		final ImmeubleBeneficiaireRF beneficiaire0 = new ImmeubleBeneficiaireRF();
		beneficiaire0.setIdRF(immeuble0.getIdRF());
		beneficiaire0.setImmeuble(immeuble0);

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("42432234");
		immeuble1.setId(servantId);

		final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
		droit0.setId(23320L);
		droit0.setMasterIdRF("28288228");
		droit0.setVersionIdRF("1");
		droit0.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit0.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit0.setMotifDebut("Achat");
		droit0.setRegime(GenrePropriete.INDIVIDUELLE);
		droit0.setPart(new Fraction(1, 1));
		droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit0.setAyantDroit(pp);
		droit0.setImmeuble(immeuble0);

		final DroitProprieteImmeubleRF droit1 = new DroitProprieteImmeubleRF();
		droit1.setId(23321L);
		droit1.setMasterIdRF("4734733");
		droit1.setVersionIdRF("1");
		droit1.setDateDebutMetier(RegDate.get(2000, 1, 1));
		droit1.setMotifDebut("Constitution de PPE");
		droit1.setRegime(GenrePropriete.FONDS_DOMINANT);
		droit1.setPart(new Fraction(3, 5));
		droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Constitution de PPE", new IdentifiantAffaireRF(21, 2000, 1, 0)));
		droit1.setAyantDroit(beneficiaire0);
		droit1.setImmeuble(immeuble1);

		final DroitProprieteVirtuelRF droit2 = new DroitProprieteVirtuelRF();
		droit2.setMasterIdRF("03030232");
		droit2.setVersionIdRF("1");
		droit2.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit2.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit2.setMotifDebut("Achat");
		droit2.setAyantDroit(pp);
		droit2.setImmeuble(immeuble1);
		droit2.setChemin(Arrays.asList(droit0, droit1));

		final LandRight landRight = LandRightBuilder.newLandRight(droit2, t -> ctbId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof VirtualLandOwnershipRight);

		final VirtualLandOwnershipRight virtualRight = (VirtualLandOwnershipRight) landRight;
		assertNotNull(virtualRight);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(virtualRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(virtualRight.getDateTo()));
		assertEquals("Achat", virtualRight.getStartReason());
		assertNull(virtualRight.getEndReason());
		assertNull(virtualRight.getCaseIdentifier());
		assertEquals(Integer.valueOf(ctbId.intValue()), virtualRight.getRightHolder().getTaxPayerNumber());
		assertEquals(servantId, virtualRight.getImmovablePropertyId());
		assertNull(virtualRight.getCommunityId());

		final List<LandOwnershipRight> paths = virtualRight.getPath();
		assertNotNull(paths);
		assertEquals(2, paths.size());

		// le chemin pp -> immeuble0
		final LandOwnershipRight path0 = paths.get(0);
		assertNotNull(path0);
		assertEquals(23320L, path0.getId());
		assertEquals(OwnershipType.SOLE_OWNERSHIP, path0.getType());
		assertShare(1, 1, path0.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(path0.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(path0.getDateTo()));
		assertEquals("Achat", path0.getStartReason());
		assertNull(path0.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", path0.getCaseIdentifier());
		assertEquals(Integer.valueOf(ctbId.intValue()), path0.getRightHolder().getTaxPayerNumber());
		assertEquals(dominantId, path0.getImmovablePropertyId());
		assertNull(path0.getCommunityId());

		// le chemin immeuble0 -> immeuble1
		final LandOwnershipRight path1 = paths.get(1);
		assertNotNull(path1);
		assertEquals(23321L, path1.getId());
		assertEquals(OwnershipType.DOMINANT_OWNERSHIP, path1.getType());
		assertShare(3, 5, path1.getShare());
		assertEquals(RegDate.get(2000, 1, 1), DataHelper.xmlToCore(path1.getDateFrom()));
		assertNull(DataHelper.xmlToCore(path1.getDateTo()));
		assertEquals("Constitution de PPE", path1.getStartReason());
		assertNull(path1.getEndReason());
		assertCaseIdentifier(21, "2000/1/0", path1.getCaseIdentifier());
		assertEquals(Long.valueOf(dominantId), path1.getRightHolder().getImmovablePropertyId());
		assertEquals(servantId, path1.getImmovablePropertyId());
		assertNull(path1.getCommunityId());
	}

	@Test
	public void testNewLandOwnershipRightCommunity() throws Exception {

		final Long communityId = 234342L;
		final long servantId = 4222L;

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(communityId);
		communaute.setIdRF("388289282");
		communaute.setType(TypeCommunaute.INDIVISION);

		final BienFondsRF servant = new BienFondsRF();
		servant.setIdRF("42432234");
		servant.setId(servantId);

		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setId(2332L);
		droit.setMasterIdRF("28288228");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2016, 11, 3));
		droit.setDateFin(RegDate.get(2017, 9, 22));
		droit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit.setMotifDebut("Achat");
		droit.setRegime(GenrePropriete.FONDS_DOMINANT);
		droit.setPart(new Fraction(3, 5));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2016, 9, 22), "Achat", new IdentifiantAffaireRF(21, 2016, 322, 3)));
		droit.setAyantDroit(communaute);
		droit.setImmeuble(servant);

		final LandRight landRight = LandRightBuilder.newLandRight(droit, t -> null, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof LandOwnershipRight);

		final LandOwnershipRight landOwnershipRight = (LandOwnershipRight) landRight;
		assertNotNull(landOwnershipRight);
		assertEquals(2332L, landOwnershipRight.getId());
		assertEquals(OwnershipType.DOMINANT_OWNERSHIP, landOwnershipRight.getType());
		assertShare(3, 5, landOwnershipRight.getShare());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(landOwnershipRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(landOwnershipRight.getDateTo()));
		assertEquals("Achat", landOwnershipRight.getStartReason());
		assertNull(landOwnershipRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", landOwnershipRight.getCaseIdentifier());
		assertEquals(communityId, landOwnershipRight.getRightHolder().getCommunityId());
		assertEquals(servantId, landOwnershipRight.getImmovablePropertyId());
		assertNull(landOwnershipRight.getCommunityId());

		final List<AcquisitionReason> reasons = landOwnershipRight.getAcquisitionReasons();
		assertNotNull(reasons);
		assertEquals(1, reasons.size());
		assertAcquisitionReason(RegDate.get(2016, 9, 22), "Achat", 21, "2016/322/3", reasons.get(0));
	}

	@Test
	public void testNewUsufructRight() throws Exception {

		final long ctbId1 = 2928282L;
		final long ctbId2 = 4573282L;

		final PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(ctbId1);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_HC, null, null));

		final PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(ctbId2);
		pp2.setPrenomUsuel("Josua");
		pp2.setNom("Bipbip");
		pp2.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		tiersMap.put(ctbId1, pp1);
		tiersMap.put(ctbId2, pp2);

		final PersonnePhysiqueRF ppRF1 = new PersonnePhysiqueRF();
		ppRF1.setId(ctbId1);

		final PersonnePhysiqueRF ppRF2 = new PersonnePhysiqueRF();
		ppRF2.setId(ctbId2);

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("a8388e8e83");
		immeuble1.setId(123456L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("a26271e8e2");
		immeuble2.setId(4783711L);
		immeuble2.setDroitsPropriete(Collections.emptySet());
		immeuble2.setServitudes(Collections.emptySet());

		final UsufruitRF usufruit = new UsufruitRF();
		usufruit.setId(2332L);
		usufruit.setDateDebut(RegDate.get(2016, 11, 3));
		usufruit.setDateFin(RegDate.get(2017, 9, 22));
		usufruit.setMotifDebut("Convention");
		usufruit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		usufruit.setDateFinMetier(RegDate.get(2017, 4, 14));
		usufruit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		usufruit.addAyantDroit(ppRF1);
		usufruit.addAyantDroit(ppRF2);
		usufruit.addImmeuble(immeuble1);
		usufruit.addImmeuble(immeuble2);

		final LandRight landRight = LandRightBuilder.newLandRight(usufruit, AyantDroitRF::getId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof UsufructRight);

		final UsufructRight usufructRight = (UsufructRight) landRight;
		assertNotNull(usufructRight);
		assertEquals(2332L, usufructRight.getId());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(usufructRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(usufructRight.getDateTo()));
		assertEquals("Convention", usufructRight.getStartReason());
		assertNull(usufructRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", usufructRight.getCaseIdentifier());

		final List<RightHolder> rightHolders = usufructRight.getRightHolders();
		assertEquals(2, rightHolders.size());
		assertEquals(Integer.valueOf((int) ctbId2), rightHolders.get(0).getTaxPayerNumber());   // CTB vaudois
		assertEquals(Integer.valueOf((int) ctbId1), rightHolders.get(1).getTaxPayerNumber());   // CTB hors-suisse

		final List<Long> immovablePropertyIds = usufructRight.getImmovablePropertyIds();
		assertEquals(2, immovablePropertyIds.size());
		assertEquals(Long.valueOf(123456L), immovablePropertyIds.get(0));
		assertEquals(Long.valueOf(4783711L), immovablePropertyIds.get(1));

		// pour des raisons de compatibilité ascendante, ces deux propriétés sont encore renseignées
		assertEquals(123456L, usufructRight.getImmovablePropertyId());
		assertEquals(Integer.valueOf((int) ctbId2), usufructRight.getRightHolder().getTaxPayerNumber());
	}

	/**
	 * [SIFISC-26200] Ce test vérifie que les usufruits sans ayant-droit (d'un point de vue métier, ça n'a aucun sens, mais il en existe : exemple immeuble CH827755834593) ne provoque pas de crash.
	 */
	@Test
	public void testNewUsufructRightWithoutRightHolder() throws Exception {

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("a8388e8e83");
		immeuble1.setId(123456L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("a26271e8e2");
		immeuble2.setId(4783711L);
		immeuble2.setDroitsPropriete(Collections.emptySet());
		immeuble2.setServitudes(Collections.emptySet());

		final UsufruitRF usufruit = new UsufruitRF();
		usufruit.setId(2332L);
		usufruit.setDateDebut(RegDate.get(2016, 11, 3));
		usufruit.setDateFin(RegDate.get(2017, 9, 22));
		usufruit.setMotifDebut("Convention");
		usufruit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		usufruit.setDateFinMetier(RegDate.get(2017, 4, 14));
		usufruit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		usufruit.setAyantDroits(Collections.emptySet());
		usufruit.addImmeuble(immeuble1);
		usufruit.addImmeuble(immeuble2);

		final LandRight landRight = LandRightBuilder.newLandRight(usufruit, AyantDroitRF::getId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof UsufructRight);

		final UsufructRight usufructRight = (UsufructRight) landRight;
		assertNotNull(usufructRight);
		assertEquals(2332L, usufructRight.getId());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(usufructRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(usufructRight.getDateTo()));
		assertEquals("Convention", usufructRight.getStartReason());
		assertNull(usufructRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", usufructRight.getCaseIdentifier());

		final List<RightHolder> rightHolders = usufructRight.getRightHolders();
		assertEquals(0, rightHolders.size());

		final List<Long> immovablePropertyIds = usufructRight.getImmovablePropertyIds();
		assertEquals(2, immovablePropertyIds.size());
		assertEquals(Long.valueOf(123456L), immovablePropertyIds.get(0));
		assertEquals(Long.valueOf(4783711L), immovablePropertyIds.get(1));

		assertEquals(123456L, usufructRight.getImmovablePropertyId());
		assertNull(usufructRight.getRightHolder());
	}

	/**
	 * <pre>
	 *                        usufruit                                +------------+
	 *                     +----------------------------------------->| Immeuble 0 |
	 *     +----------+    |                                          +------------+
	 *     |          |----+                                             |
	 *     | Tiers RF |                                                  | fond dominant (20/100)
	 *     |          |....+                                             v
	 *     +----------+    :  usufruit virtuel (usufruit * 20/100)    +------------+
	 *                     +.........................................>| Immeuble 1 |
	 *                                                                +------------+
	 * </pre>
	 */
	@Test
	public void testNewVirtualUsfructRight() throws Exception {

		final long ctbId1 = 2928282L;
		final long ctbId2 = 4573282L;

		final PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(ctbId1);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_HC, null, null));

		final PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(ctbId2);
		pp2.setPrenomUsuel("Josua");
		pp2.setNom("Bipbip");
		pp2.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		tiersMap.put(ctbId1, pp1);
		tiersMap.put(ctbId2, pp2);

		final PersonnePhysiqueRF ppRF1 = new PersonnePhysiqueRF();
		ppRF1.setId(ctbId1);

		final PersonnePhysiqueRF ppRF2 = new PersonnePhysiqueRF();
		ppRF2.setId(ctbId2);

		final BienFondsRF immeuble0 = new BienFondsRF();
		immeuble0.setIdRF("a8388e8e83");
		immeuble0.setId(123456L);
		immeuble0.setDroitsPropriete(Collections.emptySet());
		immeuble0.setServitudes(Collections.emptySet());

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("a26271e8e2");
		immeuble1.setId(4783711L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final ImmeubleBeneficiaireRF beneficiaire0 = new ImmeubleBeneficiaireRF();
		beneficiaire0.setIdRF(immeuble0.getIdRF());
		beneficiaire0.setImmeuble(immeuble0);

		final UsufruitRF usufruit = new UsufruitRF();
		usufruit.setId(23320L);
		usufruit.setDateDebut(RegDate.get(2016, 11, 3));
		usufruit.setDateFin(RegDate.get(2017, 9, 22));
		usufruit.setMotifDebut("Convention");
		usufruit.setDateDebutMetier(RegDate.get(2016, 9, 22));
		usufruit.setDateFinMetier(RegDate.get(2017, 4, 14));
		usufruit.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		usufruit.addAyantDroit(ppRF1);
		usufruit.addAyantDroit(ppRF2);
		usufruit.addImmeuble(immeuble0);
		usufruit.addImmeuble(immeuble1);

		final DroitProprieteImmeubleRF droit1 = new DroitProprieteImmeubleRF();
		droit1.setId(23321L);
		droit1.setMasterIdRF("28288228");
		droit1.setVersionIdRF("1");
		droit1.setDateDebutMetier(RegDate.get(2000, 1, 1));
		droit1.setMotifDebut("Constitution de PPE");
		droit1.setRegime(GenrePropriete.FONDS_DOMINANT);
		droit1.setPart(new Fraction(3, 5));
		droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Constitution de PPE", new IdentifiantAffaireRF(21, 2000, 1, 0)));
		droit1.setAyantDroit(beneficiaire0);
		droit1.setImmeuble(immeuble1);

		final UsufruitVirtuelRF droit2 = new UsufruitVirtuelRF();
		droit2.setMasterIdRF("478347347");
		droit2.setVersionIdRF("1");
		droit2.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droit2.setDateFinMetier(RegDate.get(2017, 4, 14));
		droit2.setMotifDebut("Achat");
		droit2.setAyantDroit(ppRF1);
		droit2.setImmeuble(immeuble1);
		droit2.setChemin(Arrays.asList(usufruit, droit1));

		final LandRight landRight = LandRightBuilder.newLandRight(droit2, AyantDroitRF::getId, rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof VirtualUsufructRight);

		final VirtualUsufructRight virtualRight = (VirtualUsufructRight) landRight;
		assertNotNull(virtualRight);
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(virtualRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(virtualRight.getDateTo()));
		assertEquals("Achat", virtualRight.getStartReason());
		assertNull(virtualRight.getEndReason());
		assertNull(virtualRight.getCaseIdentifier());
		assertEquals(Integer.valueOf((int) ctbId1), virtualRight.getRightHolder().getTaxPayerNumber());
		assertEquals(immeuble1.getId().longValue(), virtualRight.getImmovablePropertyId());

		final List<LandRight> paths = virtualRight.getPath();
		assertNotNull(paths);
		assertEquals(2, paths.size());

		// le chemin pp -> immeuble0
		final UsufructRight path0 = (UsufructRight) paths.get(0);
		assertNotNull(path0);
		assertEquals(23320L, path0.getId());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(path0.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(path0.getDateTo()));
		assertEquals("Convention", path0.getStartReason());
		assertNull(path0.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", path0.getCaseIdentifier());

		// le chemin immeuble0 -> immeuble1
		final LandOwnershipRight path1 = (LandOwnershipRight) paths.get(1);
		assertNotNull(path1);
		assertEquals(23321L, path1.getId());
		assertEquals(OwnershipType.DOMINANT_OWNERSHIP, path1.getType());
		assertShare(3, 5, path1.getShare());
		assertEquals(RegDate.get(2000, 1, 1), DataHelper.xmlToCore(path1.getDateFrom()));
		assertNull(DataHelper.xmlToCore(path1.getDateTo()));
		assertEquals("Constitution de PPE", path1.getStartReason());
		assertNull(path1.getEndReason());
		assertCaseIdentifier(21, "2000/1/0", path1.getCaseIdentifier());
		assertEquals(immeuble0.getId(), path1.getRightHolder().getImmovablePropertyId());
		assertEquals(immeuble1.getId().longValue(), path1.getImmovablePropertyId());
		assertNull(path1.getCommunityId());
	}

	@Test
	public void testNewHousingRight() throws Exception {

		final long ctbId1 = 2928282L;
		final long ctbId2 = 4573282L;

		final PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(ctbId1);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_HC, null, null));

		final PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(ctbId2);
		pp2.setPrenomUsuel("Josua");
		pp2.setNom("Bipbip");
		pp2.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		tiersMap.put(ctbId1, pp1);
		tiersMap.put(ctbId2, pp2);

		final PersonnePhysiqueRF ppRF1 = new PersonnePhysiqueRF();
		ppRF1.setId(ctbId1);

		final PersonnePhysiqueRF ppRF2 = new PersonnePhysiqueRF();
		ppRF2.setId(ctbId2);

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("a8388e8e83");
		immeuble1.setId(123456L);
		immeuble1.setDroitsPropriete(Collections.emptySet());
		immeuble1.setServitudes(Collections.emptySet());

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("a26271e8e2");
		immeuble2.setId(4783711L);
		immeuble2.setDroitsPropriete(Collections.emptySet());
		immeuble2.setServitudes(Collections.emptySet());

		final DroitHabitationRF droitHabitation = new DroitHabitationRF();
		droitHabitation.setId(2332L);
		droitHabitation.setDateDebut(RegDate.get(2016, 11, 3));
		droitHabitation.setDateFin(RegDate.get(2017, 9, 22));
		droitHabitation.setMotifDebut("Convention");
		droitHabitation.setDateDebutMetier(RegDate.get(2016, 9, 22));
		droitHabitation.setDateFinMetier(RegDate.get(2017, 4, 14));
		droitHabitation.setNumeroAffaire(new IdentifiantAffaireRF(21, 2016, 322, 3));
		droitHabitation.addAyantDroit(ppRF1);
		droitHabitation.addAyantDroit(ppRF2);
		droitHabitation.addImmeuble(immeuble1);
		droitHabitation.addImmeuble(immeuble2);

		final LandRight landRight = LandRightBuilder.newLandRight(droitHabitation, AyantDroitRF::getId, this.rightHolderComparator);
		assertNotNull(landRight);
		assertTrue(landRight instanceof HousingRight);

		final HousingRight housingRight = (HousingRight) landRight;
		assertNotNull(housingRight);
		assertEquals(2332L, housingRight.getId());
		assertEquals(RegDate.get(2016, 9, 22), DataHelper.xmlToCore(housingRight.getDateFrom()));
		assertEquals(RegDate.get(2017, 4, 14), DataHelper.xmlToCore(housingRight.getDateTo()));
		assertEquals("Convention", housingRight.getStartReason());
		assertNull(housingRight.getEndReason());
		assertCaseIdentifier(21, "2016/322/3", housingRight.getCaseIdentifier());

		final List<RightHolder> rightHolders = housingRight.getRightHolders();
		assertEquals(2, rightHolders.size());
		assertEquals(Integer.valueOf((int) ctbId2), rightHolders.get(0).getTaxPayerNumber());   // CTB vaudois
		assertEquals(Integer.valueOf((int) ctbId1), rightHolders.get(1).getTaxPayerNumber());   // CTB hors-suisse

		final List<Long> immovablePropertyIds = housingRight.getImmovablePropertyIds();
		assertEquals(2, immovablePropertyIds.size());
		assertEquals(Long.valueOf(123456L), immovablePropertyIds.get(0));
		assertEquals(Long.valueOf(4783711L), immovablePropertyIds.get(1));

		// pour des raisons de compatibilité ascendante, ces deux propriétés sont encore renseignées
		assertEquals(123456L, housingRight.getImmovablePropertyId());
		assertEquals(Integer.valueOf((int) ctbId2), housingRight.getRightHolder().getTaxPayerNumber());
	}

	public static void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	public static void assertCaseIdentifier(int officeNumber, String caseNumber, CaseIdentifier caseIdentifier) {
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