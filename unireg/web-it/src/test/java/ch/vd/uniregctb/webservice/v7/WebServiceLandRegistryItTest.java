package ch.vd.uniregctb.webservice.v7;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.uniregctb.common.DataHelper;

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
		assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=227&kr=0&n1=59&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug&sec=WUcNIuAaAn07zT5ky-Pi-g1sLdOQx2ccPgnp0PmINA0SComhxznkhXe6oY5P5pW2-q5y5NRgFZ7s4crPzqU-Yg%3D%3D",
		             realEstate.getUrlIntercapi());
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
		assertTaxEstimation(RegDate.get(1994, 1, 1), null, 570_000L, "1994", false, taxEstimates.get(0));

		final List<BuildingSetting> settings = realEstate.getBuildingSettings();
		assertEquals(1, settings.size());

		final BuildingSetting setting = settings.get(0);
		assertSetting(RegDate.get(2016, 9, 13), null, 150, noImmo, 266023444, setting);

		final List<LandRight> landRights = realEstate.getLandRights();
		assertEquals(5, landRights.size());
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), RegDate.get(2017, 10, 17), "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, "Raymonde", "Grandjean", null, 264822986L, (LandOwnershipRight) landRights.get(0));
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, 10035633, 264822986L, (LandOwnershipRight) landRights.get(1));
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, null, "Berard", null, 264822986L, (LandOwnershipRight) landRights.get(2));
		assertUsufructRight(RegDate.get(1985, 10, 10), RegDate.get(2017, 10, 17), "Convention", null, "Charles", "de Noblebois", null, null, (UsufructRight) landRights.get(3));
		assertHousingRight(RegDate.get(1999, 8, 8), null, "Convention", null, "Roland", "Proutch", null, null, (HousingRight) landRights.get(4));
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

	@Test
	public void testGetCommunityOfOwner() throws Exception {

		final int communityId = 264822986;

		final ResponseEntity<CommunityOfOwners> resp = get(CommunityOfOwners.class, MediaType.APPLICATION_XML, "/landRegistry/communityOfOwners/{id}?user=zaizzt/22", Collections.singletonMap("id", communityId));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final CommunityOfOwners community = resp.getBody();
		assertNotNull(community);
		assertEquals(communityId, community.getId());
		assertEquals(CommunityOfOwnersType.COMMUNITY_OF_HEIRS, community.getType());

		final List<RightHolder> members = community.getMembers();
		assertNotNull(members);
		assertEquals(3, members.size());
		assertRightHolderParty(10035633, members.get(0));
		assertRightHolderNaturalPerson("Raymonde", "Grandjean", null, members.get(1));
		assertRightHolderNaturalPerson(null, "Berard", null, members.get(2));
	}

	private static void assertRightHolderNaturalPerson(String firstName, String lastName, RegDate dateOfBirth, RightHolder owner) {
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		Assert.assertNotNull(identity);
		Assert.assertEquals(firstName, identity.getFirstName());
		Assert.assertEquals(lastName, identity.getLastName());
		Assert.assertEquals(dateOfBirth, DataHelper.xmlToCore(identity.getDateOfBirth()));
	}

	private static void assertRightHolderParty(long id, RightHolder owner0) {
		Assert.assertEquals(Integer.valueOf((int) id), owner0.getTaxPayerNumber());
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

	private static void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private static void assertLandOwnershipRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, String firstName, String lastName, RegDate dateOfBirth,
	                                             Long communityId, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(communityId, landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertRightHolderNaturalPerson(firstName, lastName, dateOfBirth, landRight.getRightHolder());
	}

	private static void assertUsufructRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, String firstName, String lastName, RegDate dateOfBirth, Long communityId, UsufructRight right) {
		assertNotNull(right);
		assertEquals(communityId, right.getCommunityId());
		assertDate(dateFrom, right.getDateFrom());
		assertDate(dateTo, right.getDateTo());
		assertEquals(startReason, right.getStartReason());
		assertEquals(endReason, right.getEndReason());
		assertRightHolderNaturalPerson(firstName, lastName, dateOfBirth, right.getRightHolder());
	}

	private static void assertHousingRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, String firstName, String lastName, RegDate dateOfBirth, Long communityId, HousingRight right) {
		assertNotNull(right);
		assertEquals(communityId, right.getCommunityId());
		assertDate(dateFrom, right.getDateFrom());
		assertDate(dateTo, right.getDateTo());
		assertEquals(startReason, right.getStartReason());
		assertEquals(endReason, right.getEndReason());
		assertRightHolderNaturalPerson(firstName, lastName, dateOfBirth, right.getRightHolder());
	}

	private static void assertLandOwnershipRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, long taxPayerNumber,
	                                             Long communityId, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(communityId, landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertRightHolderParty(taxPayerNumber, landRight.getRightHolder());
	}
}
