package ch.vd.uniregctb.webservice.v7;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WebServiceLandRegistryItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceLandRegistryItTest.xml";

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
	public void testGetImmovableProperty() throws Exception {

		final int noImmo = 264310664;

		final ResponseEntity<ImmovableProperty> resp = get(ImmovableProperty.class, MediaType.APPLICATION_XML, "/landRegistry/immovableProperty/{id}?user=zaizzt/22", Collections.singletonMap("id", noImmo));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final ImmovableProperty immo = resp.getBody();
		assertNotNull(immo);
		assertEquals(RealEstate.class, immo.getClass());

		final RealEstate realEstate = (RealEstate) immo;
		assertEquals(noImmo, realEstate.getId());
		assertEquals("CH785283458046", realEstate.getEgrid());
		assertEquals("todo", realEstate.getUrlIntercapi());
		assertFalse(realEstate.isCfa());

		final List<Location> locations = realEstate.getLocations();
		assertEquals(1, locations.size());
		assertLocation(locations.get(0), RegDate.get(2016, 9, 13), null, 59, null, null, null, 5706);

		final List<TotalArea> totalAreas = realEstate.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(RegDate.get(2016, 9, 13), null, 1200, totalAreas.get(0));

		final List<GroundArea> groundAreas = realEstate.getGroundAreas();
		assertEquals(1, groundAreas.size());
		assertGroundArea(RegDate.get(2016, 9, 13), null, 1050, "Place-jardin", groundAreas.get(0));

		final List<TaxEstimate> taxEstimates = realEstate.getTaxEstimates();
		assertEquals(1, taxEstimates.size());
		assertTaxEstimation(RegDate.get(2016, 9, 13), null, 570_000L, "RG94", false, taxEstimates.get(0));

		final List<BuildingSetting> settings = realEstate.getBuildingSettings();
		assertEquals(1, settings.size());

		final BuildingSetting setting = settings.get(0);
		assertSetting(RegDate.get(2016, 9, 13), null, 150, noImmo, 266023444, setting);
	}

	@Test
	public void testGetBuilding() throws Exception {

		final int buildingId = 266023444;

		final ResponseEntity<Building> resp = get(Building.class, MediaType.APPLICATION_XML, "/landRegistry/building/{id}?user=zaizzt/22", Collections.singletonMap("id", buildingId));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Building building = resp.getBody();
		assertNotNull(building);
		assertEquals(buildingId, building.getId());

		final List<BuildingDescription> descriptions = building.getDescriptions();
		assertEquals(1, descriptions.size());
		assertDescription(RegDate.get(2016, 9, 13), null, "Habitation", null, descriptions.get(0));

		final List<BuildingSetting> settings = building.getSettings();
		assertEquals(1, settings.size());
		assertSetting(RegDate.get(2016, 9, 13), null, 150, 264310664, buildingId, settings.get(0));
	}

	private static void assertDescription(RegDate dateFrom, RegDate dateTo, String type, Object area, BuildingDescription description) {
		assertNotNull(description);
		assertDate(dateFrom, description.getDateFrom());
		assertDate(dateTo, description.getDateTo());
		assertEquals(type, description.getType());
		assertEquals(area, description.getArea());
	}

	private static void assertSetting(RegDate dateFrom, RegDate dateTo, Integer area, int immoId, int buildingId, BuildingSetting setting) {
		assertNotNull(setting);
		assertDate(dateFrom, setting.getDateFrom());
		assertDate(dateTo, setting.getDateTo());
		assertEquals(area, setting.getArea());
		assertEquals(immoId, setting.getImmovablePropertyId());
		assertEquals(buildingId, setting.getBuildingId());
	}

	private static void assertTaxEstimation(RegDate dateFrom, RegDate dateTo, Long amount, String reference, boolean isInReview, TaxEstimate taxEstimate) {
		assertNotNull(taxEstimate);
		assertDate(dateFrom, taxEstimate.getDateFrom());
		assertDate(dateTo, taxEstimate.getDateTo());
		assertEquals(amount, taxEstimate.getAmount());
		assertEquals(reference, taxEstimate.getReference());
		assertEquals(isInReview, taxEstimate.isInReview());
	}

	private static void assertTotalArea(RegDate dateFrom, RegDate dateTo, int area, TotalArea totalArea) {
		assertNotNull(totalArea);
		assertDate(dateFrom, totalArea.getDateFrom());
		assertDate(dateTo, totalArea.getDateTo());
		assertEquals(area, totalArea.getArea());
	}

	private static void assertGroundArea(RegDate dateFrom, RegDate dateTo, int area, String type, GroundArea groundArea) {
		assertNotNull(groundArea);
		assertDate(dateFrom, groundArea.getDateFrom());
		assertDate(dateTo, groundArea.getDateTo());
		assertEquals(area, groundArea.getArea());
		assertEquals(type, groundArea.getType());
	}

	private static void assertLocation(Location location, RegDate dateFrom, RegDate dateTo, int parcelNumber, Integer index1, Integer index2, Integer index3, int municipalityFsoId) {
		assertNotNull(location);
		assertDate(dateFrom, location.getDateFrom());
		assertDate(dateTo, location.getDateTo());
		assertEquals(parcelNumber, location.getParcelNumber());
		assertEquals(index1, location.getIndex1());
		assertEquals(index2, location.getIndex2());
		assertEquals(index3, location.getIndex3());
		assertEquals(municipalityFsoId, location.getMunicipalityFsoId());
	}

	private static void assertDate(@Nullable RegDate expected, @Nullable Date actual) {
		if (expected == null) {
			assertNull(actual);
		}
		else {
			assertNotNull(actual);
			assertEquals(expected.year(), actual.getYear());
			assertEquals(expected.month(), actual.getMonth());
			assertEquals(expected.day(), actual.getDay());
		}
	}
}
