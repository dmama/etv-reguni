package ch.vd.uniregctb.xml.party.v5;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.CoOwnershipShare;
import ch.vd.unireg.xml.party.landregistry.v1.CondominiumOwnership;
import ch.vd.unireg.xml.party.landregistry.v1.DistinctAndPermanentRight;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.Mine;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class ImmovablePropertyBuilderTest {

	public static final CommuneRF BUSSIGNY = new CommuneRF(33, "Bussigny", MockCommune.Bussigny.getNoOFS());

	private EasementRightHolderComparator dummyRightHolderComparator;

	private static String getCapitastraUrl(Long immeubleId) {
		return "http://capitastra/" + immeubleId;
	}

	private static Long getCtbId(TiersRF tiers) {
		return null;    // on considère que le tiers RF n'est pas rapproché
	}

	@Before
	public void setUp() throws Exception {
		dummyRightHolderComparator = new EasementRightHolderComparator(id -> null,
		                                                          pp -> null,
		                                                          tiers -> null) {
			@Override
			public int compare(RightHolder o1, RightHolder o2) {
				throw new NotImplementedException();
			}
		};
	}

	@Test
	public void testNewCondominiumOwnership() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);
		final RegDate dateConstruction = dateAchat.addMonths(9);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 220, "Villa");
		final SurfaceAuSolRF surfaceAuSol1 = newSurfaceAuSolRF(dateAchat, 593, "Surface bétonnée");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, dateConstruction.getOneDayBefore(), RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 31), true, 0L, "bonne", null);
		final EstimationRF estimation1 = newEstimationRF(dateConstruction, null, RegDate.get(2004, 1, 1), null, false, 1_200_000L, "04RG", 2004);
		final BatimentRF batiment = newBatimentRF(332L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, 220);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		ppe.setId(48383L);
		ppe.setIdRF("7d7e7a7f7");
		ppe.setEgrid("rhoooo");
		ppe.setUrlIntercapi(null);
		ppe.addSituation(situation);
		ppe.addSurfaceTotale(surfaceTotale);
		ppe.setSurfacesAuSol(new HashSet<>(Arrays.asList(surfaceAuSol0, surfaceAuSol1)));
		ppe.addEstimation(estimation0);
		ppe.addEstimation(estimation1);
		ppe.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(ppe);
		ppe.setDateRadiation(null);
		ppe.setQuotePart(new Fraction(1, 23));

		final DroitProprietePersonnePhysiqueRF droit = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, ppe);

		pp.setDroitsPropriete(Collections.singleton(droit));
		pp.setServitudes(Collections.emptySet());
		ppe.setDroitsPropriete(Collections.singleton(droit));
		ppe.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final CondominiumOwnership condo = (CondominiumOwnership) ImmovablePropertyBuilder.newImmovableProperty(ppe, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(48383L, condo.getId());
		assertEquals("rhoooo", condo.getEgrid());
		assertEquals("http://capitastra/48383", condo.getUrlIntercapi());
		assertNull(condo.getCancellationDate());
		assertShare(1, 23, condo.getShare());

		final List<Location> locations = condo.getLocations();
		assertEquals(1, locations.size());
		assertLocation(dateAchat, null, 12280, 13, null, null, MockCommune.Bussigny, locations.get(0));

		final List<TotalArea> totalAreas = condo.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(dateAchat, null, 813, totalAreas.get(0));

		final List<GroundArea> groundAreas = condo.getGroundAreas();
		assertEquals(2, groundAreas.size());
		assertGroundArea(dateAchat, null, "Surface bétonnée", 593, groundAreas.get(0));
		assertGroundArea(dateAchat, null, "Villa", 220, groundAreas.get(1));

		final List<TaxEstimate> taxEstimates = condo.getTaxEstimates();
		assertEquals(2, taxEstimates.size());
		assertTaxEstimate(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 31), true, 0L, "bonne", taxEstimates.get(0));
		assertTaxEstimate(RegDate.get(2004, 1, 1), null, false, 1_200_000L, "04RG", taxEstimates.get(1));

		final List<BuildingSetting> settings = condo.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, 220, 332L, settings.get(0));

		final List<LandRight> landRights = condo.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));
	}

	@Test
	public void testNewCoOwnershipShare() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, RegDate.get(2003, 1, 1), null, false, 2_500_000L, "2003", 2003);
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final PartCoproprieteRF pcp = new PartCoproprieteRF();
		pcp.setId(480302L);
		pcp.setIdRF("7d7e7a7f7");
		pcp.setEgrid("raoul t'es là ?");
		pcp.setUrlIntercapi(null);
		pcp.addSituation(situation);
		pcp.addSurfaceTotale(surfaceTotale);
		pcp.setSurfacesAuSol(Collections.singleton(surfaceAuSol0));
		pcp.addEstimation(estimation0);
		pcp.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(pcp);
		pcp.setDateRadiation(null);
		pcp.setQuotePart(new Fraction(1, 1));

		final DroitProprietePersonnePhysiqueRF droit = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, pcp);

		pp.setDroitsPropriete(Collections.singleton(droit));
		pp.setServitudes(Collections.emptySet());
		pcp.setDroitsPropriete(Collections.singleton(droit));
		pcp.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final CoOwnershipShare coos = (CoOwnershipShare) ImmovablePropertyBuilder.newImmovableProperty(pcp, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(480302L, coos.getId());
		assertEquals("raoul t'es là ?", coos.getEgrid());
		assertEquals("http://capitastra/480302", coos.getUrlIntercapi());
		assertNull(coos.getCancellationDate());
		assertShare(1, 1, coos.getShare());

		final List<Location> locations = coos.getLocations();
		assertEquals(1, locations.size());
		assertLocation(dateAchat, null, 12280, 13, null, null, MockCommune.Bussigny, locations.get(0));

		final List<TotalArea> totalAreas = coos.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(dateAchat, null, 813, totalAreas.get(0));

		final List<GroundArea> groundAreas = coos.getGroundAreas();
		assertEquals(1, groundAreas.size());
		assertGroundArea(dateAchat, null, "Immeuble", 800, groundAreas.get(0));

		final List<TaxEstimate> taxEstimates = coos.getTaxEstimates();
		assertEquals(1, taxEstimates.size());
		assertTaxEstimate(RegDate.get(2003, 1, 1), null, false, 2_500_000L, "2003", taxEstimates.get(0));

		final List<BuildingSetting> settings = coos.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));

		final List<LandRight> landRights = coos.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));
	}

	@Test
	public void testNewDistinctAndPermanentRight() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, RegDate.get(2003, 1, 1), null, false, 2_500_000L, "2003", 2003);
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final DroitDistinctEtPermanentRF ddp = new DroitDistinctEtPermanentRF();
		ddp.setId(480302L);
		ddp.setIdRF("7d7e7a7f7");
		ddp.setEgrid("raoul t'es là ?");
		ddp.setUrlIntercapi(null);
		ddp.addSituation(situation);
		ddp.addSurfaceTotale(surfaceTotale);
		ddp.setSurfacesAuSol(Collections.singleton(surfaceAuSol0));
		ddp.addEstimation(estimation0);
		ddp.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(ddp);
		ddp.setDateRadiation(null);

		final DroitProprietePersonnePhysiqueRF droit = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, ddp);

		pp.setDroitsPropriete(Collections.singleton(droit));
		pp.setServitudes(Collections.emptySet());
		ddp.setDroitsPropriete(Collections.singleton(droit));
		ddp.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final DistinctAndPermanentRight dpr = (DistinctAndPermanentRight) ImmovablePropertyBuilder.newImmovableProperty(ddp, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(480302L, dpr.getId());
		assertEquals("raoul t'es là ?", dpr.getEgrid());
		assertEquals("http://capitastra/480302", dpr.getUrlIntercapi());
		assertNull(dpr.getCancellationDate());

		final List<Location> locations = dpr.getLocations();
		assertEquals(1, locations.size());
		assertLocation(dateAchat, null, 12280, 13, null, null, MockCommune.Bussigny, locations.get(0));

		final List<TotalArea> totalAreas = dpr.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(dateAchat, null, 813, totalAreas.get(0));

		final List<GroundArea> groundAreas = dpr.getGroundAreas();
		assertEquals(1, groundAreas.size());
		assertGroundArea(dateAchat, null, "Immeuble", 800, groundAreas.get(0));

		final List<TaxEstimate> taxEstimates = dpr.getTaxEstimates();
		assertEquals(1, taxEstimates.size());
		assertTaxEstimate(RegDate.get(2003, 1, 1), null, false, 2_500_000L, "2003", taxEstimates.get(0));

		final List<BuildingSetting> settings = dpr.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));

		final List<LandRight> landRights = dpr.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));
	}

	@Test
	public void testNewMine() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, RegDate.get(2003, 1, 1), null, false, 2_500_000L, "tatatiiiin !", null);
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final MineRF m = new MineRF();
		m.setId(480302L);
		m.setIdRF("7d7e7a7f7");
		m.setEgrid("la fuite vibrante du câble");
		m.setUrlIntercapi(null);
		m.addSituation(situation);
		m.addSurfaceTotale(surfaceTotale);
		m.setSurfacesAuSol(Collections.singleton(surfaceAuSol0));
		m.addEstimation(estimation0);
		m.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(m);
		m.setDateRadiation(null);

		final DroitProprietePersonnePhysiqueRF droit = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, m);

		pp.setDroitsPropriete(Collections.singleton(droit));
		pp.setServitudes(Collections.emptySet());
		m.setDroitsPropriete(Collections.singleton(droit));
		m.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final Mine mine = (Mine) ImmovablePropertyBuilder.newImmovableProperty(m, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(480302L, mine.getId());
		assertEquals("la fuite vibrante du câble", mine.getEgrid());
		assertEquals("http://capitastra/480302", mine.getUrlIntercapi());
		assertNull(mine.getCancellationDate());

		final List<Location> locations = mine.getLocations();
		assertEquals(1, locations.size());
		assertLocation(dateAchat, null, 12280, 13, null, null, MockCommune.Bussigny, locations.get(0));

		final List<TotalArea> totalAreas = mine.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(dateAchat, null, 813, totalAreas.get(0));

		final List<GroundArea> groundAreas = mine.getGroundAreas();
		assertEquals(1, groundAreas.size());
		assertGroundArea(dateAchat, null, "Immeuble", 800, groundAreas.get(0));

		final List<TaxEstimate> taxEstimates = mine.getTaxEstimates();
		assertEquals(1, taxEstimates.size());
		assertTaxEstimate(RegDate.get(2003, 1, 1), null, false, 2_500_000L, "tatatiiiin !", taxEstimates.get(0));

		final List<BuildingSetting> settings = mine.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));

		final List<LandRight> landRights = mine.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));
	}

	@Test
	public void testNewRealEstate() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);
		final RegDate dateConstruction = dateAchat.addMonths(9);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 220, "Villa");
		final SurfaceAuSolRF surfaceAuSol1 = newSurfaceAuSolRF(dateAchat, 593, "Surface bétonnée");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, dateConstruction.getOneDayBefore(), RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 31), true, 0L, "RG03", 2003);
		final EstimationRF estimation1 = newEstimationRF(dateConstruction, null, RegDate.get(2004, 1, 1), null, false, 1_200_000L, "2004", 2004);
		final BatimentRF batiment = newBatimentRF(332L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, 220);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final BienFondRF bienFond = new BienFondRF();
		bienFond.setId(48383L);
		bienFond.setIdRF("7d7e7a7f7");
		bienFond.setEgrid("rhoooo");
		bienFond.setUrlIntercapi(null);
		bienFond.addSituation(situation);
		bienFond.addSurfaceTotale(surfaceTotale);
		bienFond.setSurfacesAuSol(new HashSet<>(Arrays.asList(surfaceAuSol0, surfaceAuSol1)));
		bienFond.addEstimation(estimation0);
		bienFond.addEstimation(estimation1);
		bienFond.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(bienFond);
		bienFond.setDateRadiation(null);

		final DroitProprietePersonnePhysiqueRF droit = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, bienFond);

		pp.setDroitsPropriete(Collections.singleton(droit));
		pp.setServitudes(Collections.emptySet());
		bienFond.setDroitsPropriete(Collections.singleton(droit));
		bienFond.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final RealEstate realEstate = (RealEstate) ImmovablePropertyBuilder.newImmovableProperty(bienFond, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(48383L, realEstate.getId());
		assertEquals("rhoooo", realEstate.getEgrid());
		assertEquals("http://capitastra/48383", realEstate.getUrlIntercapi());
		assertNull(realEstate.getCancellationDate());

		final List<Location> locations = realEstate.getLocations();
		assertEquals(1, locations.size());
		assertLocation(dateAchat, null, 12280, 13, null, null, MockCommune.Bussigny, locations.get(0));

		final List<TotalArea> totalAreas = realEstate.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(dateAchat, null, 813, totalAreas.get(0));

		final List<GroundArea> groundAreas = realEstate.getGroundAreas();
		assertEquals(2, groundAreas.size());
		assertGroundArea(dateAchat, null, "Surface bétonnée", 593, groundAreas.get(0));
		assertGroundArea(dateAchat, null, "Villa", 220, groundAreas.get(1));

		final List<TaxEstimate> taxEstimates = realEstate.getTaxEstimates();
		assertEquals(2, taxEstimates.size());
		assertTaxEstimate(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 31), true, 0L, "RG03", taxEstimates.get(0));
		assertTaxEstimate(RegDate.get(2004, 1, 1), null, false, 1_200_000L, "2004", taxEstimates.get(1));

		final List<BuildingSetting> settings = realEstate.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, 220, 332L, settings.get(0));

		final List<LandRight> landRights = realEstate.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));
	}

	/**
	 * [SIFISC-23985] Vérifie que les droits de propriété de l'immeuble sur d'autres immeubles sont bien exposés.
	 */
	@Test
	public void testImmeubleAvecDroitSurUnAutreImmeuble() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);
		final RegDate dateConstruction = dateAchat.addMonths(9);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setNom("Ramon");

		final BienFondRF bienFond = new BienFondRF();
		bienFond.setId(723721L);

		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		ppe.setId(48383L);
		ppe.setIdRF("7d7e7a7f7");
		ppe.setEgrid("rhoooo");
		ppe.setUrlIntercapi(null);
		ppe.addSituation(situation);
		ppe.setSurfacesTotales(Collections.emptySet());
		ppe.setSurfacesAuSol(Collections.emptySet());
		ppe.setEstimations(Collections.emptySet());
		ppe.setImplantations(Collections.emptySet());
		ppe.setDateRadiation(null);
		ppe.setQuotePart(new Fraction(1, 23));

		final ImmeubleBeneficiaireRF beneficiaire = new ImmeubleBeneficiaireRF();
		beneficiaire.setIdRF(ppe.getIdRF());
		beneficiaire.setImmeuble(ppe);
		ppe.setEquivalentBeneficiaire(beneficiaire);

		// la PPE possède une part du bien-fonds
		final DroitProprieteImmeubleRF droit0 = newDroitProprieteImm("0293929", new Fraction(1,30), GenrePropriete.PPE, RegDate.get(1993,5,13), "Consitution de PPE", beneficiaire, bienFond);
		beneficiaire.setDroitsPropriete(Collections.singleton(droit0));
		beneficiaire.setServitudes(Collections.emptySet());
		bienFond.setDroitsPropriete(Collections.singleton(droit0));
		bienFond.setServitudes(Collections.emptySet());

		// la personne physique possède la PPE
		final DroitProprietePersonnePhysiqueRF droit1 = newDroitProprietePP("389239478", new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2000, 1, 1), "Achat", pp, ppe);
		pp.setDroitsPropriete(Collections.singleton(droit1));
		pp.setServitudes(Collections.emptySet());
		ppe.setDroitsPropriete(Collections.singleton(droit1));
		ppe.setServitudes(Collections.emptySet());

		// conversion core -> ws
		final CondominiumOwnership condo = (CondominiumOwnership) ImmovablePropertyBuilder.newImmovableProperty(ppe, ImmovablePropertyBuilderTest::getCapitastraUrl, ImmovablePropertyBuilderTest::getCtbId, dummyRightHolderComparator);
		assertEquals(48383L, condo.getId());
		assertEquals("rhoooo", condo.getEgrid());
		assertEquals("http://capitastra/48383", condo.getUrlIntercapi());
		assertNull(condo.getCancellationDate());
		assertShare(1, 23, condo.getShare());

		// le droit entre la personne physique et la PPE
		final List<LandRight> landRights = condo.getLandRights();
		assertEquals(1, landRights.size());
		assertLandOwnershipRight(null, "Ramon", null, 1, 1, OwnershipType.SOLE_OWNERSHIP, (LandOwnershipRight) landRights.get(0));

		// le droit entre la PPE (dominant) et le bien-fonds (servant)
		final List<LandRight> landRightsFrom = condo.getLandRightsFrom();
		assertEquals(1, landRightsFrom.size());
		assertLandOwnershipRight(48383L, 723721L, RegDate.get(1993, 5, 13), 1, 30, OwnershipType.CONDOMINIUM_OWNERSHIP, (LandOwnershipRight) landRightsFrom.get(0));
	}

	private static void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private static void assertSetting(RegDate dateFrom, RegDate dateTo, Integer area, long buildingId, BuildingSetting setting) {
		assertNotNull(setting);
		assertEquals(dateFrom, DataHelper.xmlToCore(setting.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(setting.getDateTo()));
		assertEquals(area, setting.getArea());
		assertEquals(buildingId, setting.getBuildingId());
	}

	private static void assertTaxEstimate(RegDate dateFrom, RegDate dateTo, boolean inReview, Long amount, String reference, TaxEstimate taxEstimate) {
		assertNotNull(taxEstimate);
		assertEquals(dateFrom, DataHelper.xmlToCore(taxEstimate.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(taxEstimate.getDateTo()));
		assertEquals(inReview, taxEstimate.isInReview());
		assertEquals(amount, taxEstimate.getAmount());
		assertEquals(reference, taxEstimate.getReference());
	}

	private static void assertGroundArea(RegDate dateFrom, RegDate dateTo, String type, int area, GroundArea groundArea) {
		assertNotNull(groundArea);
		assertEquals(dateFrom, DataHelper.xmlToCore(groundArea.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(groundArea.getDateTo()));
		assertEquals(type, groundArea.getType());
		assertEquals(area, groundArea.getArea());
	}

	private static void assertTotalArea(RegDate dateFrom, RegDate dateTo, int area, TotalArea totalArea) {
		assertNotNull(totalArea);
		assertEquals(dateFrom, DataHelper.xmlToCore(totalArea.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(totalArea.getDateTo()));
		assertEquals(area, totalArea.getArea());
	}

	private static void assertLocation(RegDate dateFrom, RegDate dateTo, int parcelNumber, Integer index1, Integer index2, Integer index3, MockCommune commune, Location location) {
		assertNotNull(location);
		assertEquals(dateFrom, DataHelper.xmlToCore(location.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(location.getDateTo()));
		assertEquals(parcelNumber, location.getParcelNumber());
		assertEquals(index1, location.getIndex1());
		assertEquals(index2, location.getIndex2());
		assertEquals(index3, location.getIndex3());
		assertEquals(commune.getNoOFS(), location.getMunicipalityFsoId());
	}

	@NotNull
	private static ImplantationRF newImplantationRF(RegDate dateAchat, BatimentRF batiment, Integer surface) {
		final ImplantationRF implantation = new ImplantationRF();
		implantation.setDateDebut(dateAchat);
		implantation.setBatiment(batiment);
		implantation.setSurface(surface);
		return implantation;
	}

	@NotNull
	private static BatimentRF newBatimentRF(long id) {
		final BatimentRF batiment = new BatimentRF();
		batiment.setId(id);
		return batiment;
	}

	@NotNull
	private static EstimationRF newEstimationRF(RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, boolean enRevision, long montant, String reference, Integer anneeReference) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setDateDebut(dateDebut);
		estimation.setDateFin(dateFin);
		estimation.setDateDebutMetier(dateDebutMetier);
		estimation.setDateFinMetier(dateFinMetier);
		estimation.setEnRevision(enRevision);
		estimation.setMontant(montant);
		estimation.setReference(reference);
		estimation.setAnneeReference(anneeReference);
		return estimation;
	}

	@NotNull
	private static SurfaceAuSolRF newSurfaceAuSolRF(RegDate dateAchat, int surface, String type) {
		final SurfaceAuSolRF surfaceAuSol0 = new SurfaceAuSolRF();
		surfaceAuSol0.setDateDebut(dateAchat);
		surfaceAuSol0.setSurface(surface);
		surfaceAuSol0.setType(type);
		return surfaceAuSol0;
	}

	@NotNull
	private static SurfaceTotaleRF newSurfaceTotaleRF(RegDate dateAchat, int surface) {
		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setDateDebut(dateAchat);
		surfaceTotale.setSurface(surface);
		return surfaceTotale;
	}

	@NotNull
	private static SituationRF newSituationRF(RegDate dateAchat, CommuneRF commune, int noParcelle, int index1) {
		final SituationRF situation = new SituationRF();
		situation.setDateDebut(dateAchat);
		situation.setCommune(commune);
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		return situation;
	}

	private static void assertLandOwnershipRight(String firstName, String lastname, Object dateOfBirth, int numerator, int denominator, OwnershipType type, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(type, landRight.getType());
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) landRight.getRightHolder().getIdentity();
		assertEquals(firstName, identity.getFirstName());
		assertEquals(lastname, identity.getLastName());
		assertEquals(dateOfBirth, DataHelper.xmlToCore(identity.getDateOfBirth()));
		assertShare(numerator, denominator, landRight.getShare());
		assertEquals(RegDate.get(2000, 1, 1), DataHelper.xmlToCore(landRight.getDateFrom()));
	}

	private static void assertLandOwnershipRight(long immeubleDominantId, long immeubleServantId, RegDate dateFrom, int numerator, int denominator, OwnershipType type, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(type, landRight.getType());
		assertEquals(Long.valueOf(immeubleDominantId), landRight.getRightHolder().getImmovablePropertyId());
		assertEquals(immeubleServantId, landRight.getImmovablePropertyId());
		assertNull(landRight.getRightHolder().getIdentity());
		assertShare(numerator, denominator, landRight.getShare());
		assertEquals(dateFrom, DataHelper.xmlToCore(landRight.getDateFrom()));
		assertNull(DataHelper.xmlToCore(landRight.getDateTo()));
	}

	@NotNull
	private static DroitProprietePersonnePhysiqueRF newDroitProprietePP(String masterIdRF, Fraction part, GenrePropriete regime, RegDate dateDebut, String motifDebut, PersonnePhysiqueRF pp, ImmeubleRF immeuble) {
		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setAyantDroit(pp);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.setImmeuble(immeuble);
		droit.setMasterIdRF(masterIdRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutMetier(dateDebut);
		droit.setMotifDebut(motifDebut);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebut, motifDebut, null));
		return droit;
	}

	@NotNull
	private static DroitProprieteImmeubleRF newDroitProprieteImm(String masterIdRF, Fraction part, GenrePropriete regime, RegDate dateDebut, String motifDebut, ImmeubleBeneficiaireRF beneficiaire, ImmeubleRF immeuble) {
		final DroitProprieteImmeubleRF droit = new DroitProprieteImmeubleRF();
		droit.setAyantDroit(beneficiaire);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.setImmeuble(immeuble);
		droit.setMasterIdRF(masterIdRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutMetier(dateDebut);
		droit.setMotifDebut(motifDebut);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebut, motifDebut, null));
		return droit;
	}
}