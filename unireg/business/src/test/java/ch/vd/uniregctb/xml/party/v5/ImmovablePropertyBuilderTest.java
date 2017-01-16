package ch.vd.uniregctb.xml.party.v5;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.CoOwnershipShare;
import ch.vd.unireg.xml.party.landregistry.v1.CondominiumOwnership;
import ch.vd.unireg.xml.party.landregistry.v1.DistinctAndPermanentRight;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.Mine;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class ImmovablePropertyBuilderTest {

	public static final CommuneRF BUSSIGNY = new CommuneRF(33, "Bussigny", MockCommune.Bussigny.getNoOFS());

	@Test
	public void testNewCondominiumOwnership() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);
		final RegDate dateConstruction = dateAchat.addMonths(6);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 220, "Villa");
		final SurfaceAuSolRF surfaceAuSol1 = newSurfaceAuSolRF(dateAchat, 593, "Surface bétonnée");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, dateConstruction.getOneDayBefore(), true, 0L, "bonne");
		final EstimationRF estimation1 = newEstimationRF(dateConstruction, null, false, 1_200_000L, "très bonne");
		final BatimentRF batiment = newBatimentRF(332L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, 220);

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

		// conversion core -> ws
		final CondominiumOwnership condo = (CondominiumOwnership) ImmovablePropertyBuilder.newImmovableProperty(ppe);
		assertEquals(48383L, condo.getId());
		assertEquals("rhoooo", condo.getEgrid());
		assertEquals("todo", condo.getUrlIntercapi());
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
		assertTaxEstimate(dateAchat, dateConstruction.getOneDayBefore(), true, 0L, "bonne", taxEstimates.get(0));
		assertTaxEstimate(dateConstruction, null, false, 1_200_000L, "très bonne", taxEstimates.get(1));

		final List<BuildingSetting> settings = condo.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, 220, 332L, settings.get(0));
	}

	@Test
	public void testNewCoOwnershipShare() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, false, 2_500_000L, "et chez vous, ça va ?");
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

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

		// conversion core -> ws
		final CoOwnershipShare coos = (CoOwnershipShare) ImmovablePropertyBuilder.newImmovableProperty(pcp);
		assertEquals(480302L, coos.getId());
		assertEquals("raoul t'es là ?", coos.getEgrid());
		assertEquals("todo", coos.getUrlIntercapi());
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
		assertTaxEstimate(dateAchat, null, false, 2_500_000L, "et chez vous, ça va ?", taxEstimates.get(0));

		final List<BuildingSetting> settings = coos.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));
	}

	@Test
	public void testNewDistinctAndPermanentRight() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, false, 2_500_000L, "et chez vous, ça va ?");
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

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

		// conversion core -> ws
		final DistinctAndPermanentRight dpr = (DistinctAndPermanentRight) ImmovablePropertyBuilder.newImmovableProperty(ddp);
		assertEquals(480302L, dpr.getId());
		assertEquals("raoul t'es là ?", dpr.getEgrid());
		assertEquals("todo", dpr.getUrlIntercapi());
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
		assertTaxEstimate(dateAchat, null, false, 2_500_000L, "et chez vous, ça va ?", taxEstimates.get(0));

		final List<BuildingSetting> settings = dpr.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));
	}

	@Test
	public void testNewMine() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 800, "Immeuble");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, null, false, 2_500_000L, "tatatiiiin !");
		final BatimentRF batiment = newBatimentRF(458238L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, null);

		final MineRF ddp = new MineRF();
		ddp.setId(480302L);
		ddp.setIdRF("7d7e7a7f7");
		ddp.setEgrid("la fuite vibrante du câble");
		ddp.setUrlIntercapi(null);
		ddp.addSituation(situation);
		ddp.addSurfaceTotale(surfaceTotale);
		ddp.setSurfacesAuSol(Collections.singleton(surfaceAuSol0));
		ddp.addEstimation(estimation0);
		ddp.setImplantations(Collections.singleton(implantation));
		implantation.setImmeuble(ddp);
		ddp.setDateRadiation(null);

		// conversion core -> ws
		final Mine mine = (Mine) ImmovablePropertyBuilder.newImmovableProperty(ddp);
		assertEquals(480302L, mine.getId());
		assertEquals("la fuite vibrante du câble", mine.getEgrid());
		assertEquals("todo", mine.getUrlIntercapi());
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
		assertTaxEstimate(dateAchat, null, false, 2_500_000L, "tatatiiiin !", taxEstimates.get(0));

		final List<BuildingSetting> settings = mine.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, null, 458238L, settings.get(0));
	}

	@Test
	public void testNewRealEstate() throws Exception {

		final RegDate dateAchat = RegDate.get(2003, 4, 2);
		final RegDate dateConstruction = dateAchat.addMonths(6);

		// données core
		final SituationRF situation = newSituationRF(dateAchat, BUSSIGNY, 12280, 13);
		final SurfaceTotaleRF surfaceTotale = newSurfaceTotaleRF(dateAchat, 813);
		final SurfaceAuSolRF surfaceAuSol0 = newSurfaceAuSolRF(dateAchat, 220, "Villa");
		final SurfaceAuSolRF surfaceAuSol1 = newSurfaceAuSolRF(dateAchat, 593, "Surface bétonnée");
		final EstimationRF estimation0 = newEstimationRF(dateAchat, dateConstruction.getOneDayBefore(), true, 0L, "bonne");
		final EstimationRF estimation1 = newEstimationRF(dateConstruction, null, false, 1_200_000L, "très bonne");
		final BatimentRF batiment = newBatimentRF(332L);
		final ImplantationRF implantation = newImplantationRF(dateAchat, batiment, 220);

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

		// conversion core -> ws
		final RealEstate realEstate = (RealEstate) ImmovablePropertyBuilder.newImmovableProperty(bienFond);
		assertEquals(48383L, realEstate.getId());
		assertEquals("rhoooo", realEstate.getEgrid());
		assertEquals("todo", realEstate.getUrlIntercapi());
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
		assertTaxEstimate(dateAchat, dateConstruction.getOneDayBefore(), true, 0L, "bonne", taxEstimates.get(0));
		assertTaxEstimate(dateConstruction, null, false, 1_200_000L, "très bonne", taxEstimates.get(1));

		final List<BuildingSetting> settings = realEstate.getBuildingSettings();
		assertEquals(1, settings.size());
		assertSetting(dateAchat, null, 220, 332L, settings.get(0));
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
	private static EstimationRF newEstimationRF(RegDate dateDebut, RegDate dateFin, boolean enRevision, long montant, String reference) {
		final EstimationRF estimation0 = new EstimationRF();
		estimation0.setDateDebut(dateDebut);
		estimation0.setDateFin(dateFin);
		estimation0.setEnRevision(enRevision);
		estimation0.setMontant(montant);
		estimation0.setReference(reference);
		return estimation0;
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
}