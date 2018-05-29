package ch.vd.unireg.webservice.v7;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.landtaxlightening.v1.HousingActData;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.landtaxlightening.v1.UseData;
import ch.vd.unireg.xml.party.landtaxlightening.v1.VirtualLandTaxLightening;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;

import static ch.vd.unireg.webservice.v7.WebServiceGetPartyItTest.buildUriAndParams;
import static ch.vd.unireg.webservice.v7.WebServiceLandRegistryItTest.assertDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WebServiceLandTaxLighteningsItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceLandTaxLighteningsItTest.xml";

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
	public void testGetImmovablePropertyWithTaxLightenings() throws Exception {

		final int noOrg = 312;
		final int noImm = 264310664;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noOrg, EnumSet.of(PartyPart.LAND_TAX_LIGHTENINGS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertTrue(party instanceof Corporation);
		final Corporation corp = (Corporation) party;

		final List<IfoncExemption> exemptions = corp.getIfoncExemptions();
		assertNotNull(exemptions);
		assertEquals(1, exemptions.size());
		assertExemption(RegDate.get(2010, 1, 1), RegDate.get(2018, 12, 31), BigDecimal.valueOf(4523, 2), noImm, exemptions.get(0));

		final List<IciAbatement> abatements = corp.getIciAbatements();
		assertNotNull(abatements);
		assertEquals(2, abatements.size());
		assertAbatement(noImm, RegDate.get(2013, 3, 22), null,
		                12000L, 6500L, 230L, BigDecimal.valueOf(80), BigDecimal.valueOf(60),
		                11000L, 5000L, 250L, BigDecimal.valueOf(20), BigDecimal.valueOf(40),
		                RegDate.get(2015, 3, 1), RegDate.get(2020, 2, 25), BigDecimal.valueOf(825, 2), abatements.get(0));
		assertAbatement(noImm, RegDate.get(2015, 6, 1), RegDate.get(2015, 6, 30),
		                null, null, null, null, null,
		                null, null, null, null, null,
		                null, null, null, abatements.get(1));

		final List<IciAbatementRequest> requests = corp.getIciAbatementRequests();
		assertNotNull(requests);
		assertEquals(1, requests.size());
		assertAbatementRequest(RegDate.get(2016, 2, 3), RegDate.get(2016, 6, 30), RegDate.get(2016, 7, 1), RegDate.get(2016, 11, 2), 2015, 1, 264310664, requests.get(0));
	}

	/**
	 * [IMM-1206] Vérifie que la part VIRTUAL_LAND_TAX_LIGHTENINGS fonctionne bien.
	 */
	@Test
	public void testGetImmovablePropertyWithVirtualTaxLightenings() throws Exception {

		final int noOrg = 312;
		final int noImm = 264310664;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noOrg, EnumSet.of(PartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertTrue(party instanceof Corporation);
		final Corporation corp = (Corporation) party;

		final List<VirtualLandTaxLightening> lightenings = corp.getVirtualLandTaxLightenings();
		assertNotNull(lightenings);
		assertEquals(2, lightenings.size());

		final VirtualLandTaxLightening lightening0 = lightenings.get(0);
		assertNotNull(lightening0);
		assertDate(RegDate.get(2013, 3, 3), lightening0.getDateFrom());   // date de la fusion
		assertDate(null, lightening0.getDateTo());
		assertEquals(264310664, lightening0.getImmovablePropertyId());
		assertExemption(RegDate.get(2010,1,1), null, BigDecimal.valueOf(1277, 2), 264310664, (IfoncExemption) lightening0.getReference());

		final VirtualLandTaxLightening lightening1 = lightenings.get(1);
		assertNotNull(lightening1);
		assertDate(RegDate.get(2015, 6, 1), lightening1.getDateFrom());
		assertDate(null, lightening1.getDateTo());
		assertEquals(264310664, lightening1.getImmovablePropertyId());
		assertAbatement(noImm, RegDate.get(2015, 6, 1), null,
		                5000L, 500L, 30L, BigDecimal.valueOf(75), BigDecimal.valueOf(65),
		                1000L, 500L, 50L, BigDecimal.valueOf(30), BigDecimal.valueOf(25),
		                RegDate.get(2005, 2, 11), RegDate.get(2019, 12, 31), BigDecimal.valueOf(2038, 2), (IciAbatement) lightening1.getReference());
	}

	private static void assertAbatement(int noImm, RegDate dateFrom, RegDate dateTo, @Nullable Long rentalIncome, @Nullable Long rentalVolume, @Nullable Long rentalArea, BigDecimal rentalDeclaredPercent, BigDecimal rentalApprovedPercent,
	                                    @Nullable Long ownUseIncome, @Nullable Long ownUseVolume, @Nullable Long ownUseArea, BigDecimal ownUseDeclaredPercent, BigDecimal ownUseApprovedPercent, RegDate grantDate, RegDate expireDate,
	                                    BigDecimal socialNaturePercent, IciAbatement abatement) {
		assertDate(dateFrom, abatement.getDateFrom());
		assertDate(dateTo, abatement.getDateTo());
		assertEquals(noImm, abatement.getImmovablePropertyId());
		assertUseData(rentalIncome, rentalVolume, rentalArea, rentalDeclaredPercent, rentalApprovedPercent, abatement.getRentalUse());
		assertUseData(ownUseIncome, ownUseVolume, ownUseArea, ownUseDeclaredPercent, ownUseApprovedPercent, abatement.getOwnUse());
		assertHousingAct(grantDate, expireDate, socialNaturePercent, abatement.getHousingAct());
	}

	private static void assertAbatementRequest(RegDate sendDate, RegDate deadline, RegDate reminderDate, RegDate returnDate, int taxPeriod, int sequenceNumber, int immovablePropId, IciAbatementRequest request) {
		assertNotNull(request);
		assertDate(sendDate, request.getSendDate());
		assertDate(deadline, request.getDeadline());
		assertDate(reminderDate, request.getReminderDate());
		assertDate(returnDate, request.getReturnDate());
		assertEquals(taxPeriod, request.getTaxPeriod());
		assertEquals(sequenceNumber, request.getSequenceNumber());
		assertEquals(immovablePropId, request.getImmovablePropertyId());
	}

	private static void assertHousingAct(@Nullable RegDate grantDate, @Nullable RegDate expireDate, @Nullable BigDecimal socialNaturePercent, @Nullable HousingActData housingAct) {
		if (grantDate == null && expireDate == null && socialNaturePercent == null) {
			assertNull(housingAct);
		}
		else {
			assertNotNull(housingAct);
			assertDate(grantDate, housingAct.getGrantDate());
			assertDate(expireDate, housingAct.getExpiryDate());
			assertEquals(socialNaturePercent, housingAct.getSocialNaturePercent());
		}
	}

	private static void assertUseData(@Nullable Long income, @Nullable Long volume, @Nullable Long area, @Nullable BigDecimal declaredPercent, @Nullable BigDecimal approvedPercent, @Nullable UseData useData) {
		if (income == null && volume == null && area == null && declaredPercent == null && approvedPercent == null) {
			assertNull(useData);
		}
		else {
			assertNotNull(useData);
			assertEquals(income, useData.getIncome());
			assertEquals(volume, useData.getVolume());
			assertEquals(area, useData.getArea());
			assertEquals(declaredPercent, useData.getDeclaredPercent());
			assertEquals(approvedPercent, useData.getApprovedPercent());
		}
	}

	private static void assertExemption(RegDate dateFrom, RegDate dateTo, BigDecimal percent, long immovablePropId, IfoncExemption exemption) {
		assertNotNull(exemption);
		assertDate(dateFrom, exemption.getDateFrom());
		assertDate(dateTo, exemption.getDateTo());
		assertEquals(percent, exemption.getExemptionPercent());
		assertEquals(immovablePropId, exemption.getImmovablePropertyId());
	}
}
